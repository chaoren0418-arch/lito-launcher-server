package com.lineage.server.clientpackets;

import static com.lineage.server.model.skill.L1SkillId.AREA_OF_SILENCE;
import static com.lineage.server.model.skill.L1SkillId.SILENCE;
import static com.lineage.server.model.skill.L1SkillId.STATUS_POISON_SILENCE;

import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.lineage.echo.ClientExecutor;
import com.lineage.server.datatables.PolyTable;
import com.lineage.server.datatables.SkillsTable;
import com.lineage.server.model.ClientActionTimeline;
import com.lineage.server.model.L1Character;
import com.lineage.server.model.L1Object;
import com.lineage.server.model.L1PolyMorph;
import com.lineage.server.model.PlayerActionLock;
import com.lineage.server.model.Instance.L1PcInstance;
import com.lineage.server.model.skill.L1SkillUse;
import com.lineage.server.serverpackets.S_ServerMessage;
import com.lineage.server.templates.L1Skills;
import com.lineage.server.utils.CheckUtil;
import com.lineage.server.utils.PlayerTargetPositionFence;
import com.lineage.server.utils.TeleportTrace;
import com.lineage.server.world.World;

/**
 * 要求使用技能
 * 
 * @author daien
 * 
 */
public class C_UseSkill extends ClientBasePacket {

	private static final Logger _log = LogManager.getLogger(C_UseSkill.class);
	private static final AtomicLong START_COUNT = new AtomicLong();
	private static final AtomicLong REJECT_COUNT = new AtomicLong();
	private static final AtomicLong SKIP_COUNT = new AtomicLong();
	private static final AtomicLong ERROR_COUNT = new AtomicLong();

	/*
	 * public C_UseSkill() { }
	 * 
	 * public C_UseSkill(final byte[] abyte0, final ClientExecutor client) { super(abyte0); try { this.start(abyte0, client);
	 * 
	 * } catch (final Exception e) { _log.error(e.getLocalizedMessage(), e); } }
	 */

	//@Override
	public void start(final byte[] decrypt, final ClientExecutor client) {
		START_COUNT.incrementAndGet();
		try {
			// 資料載入
			this.read(decrypt);

			if (decrypt == null || decrypt.length < 3 || client == null || !client.isConnected()) {
				REJECT_COUNT.incrementAndGet();
				return;
			}
			final L1PcInstance pc = client.getActiveChar();

			if (pc == null) {
				REJECT_COUNT.incrementAndGet();
				return;
			}
			TeleportTrace.event(pc, "SKILL_REQUEST", "skillId",
					((decrypt[1] & 255) << 3) + (decrypt[2] & 255) + 1);

			/*
			 * final long noetime = System.currentTimeMillis(); if (noetime - pc.get_spr_skill_time() <= SprTable.get().spr_skill_speed(pc.getTempCharGfx())) { if (!pc.isGm()) { pc.getNetConnection().kick(); return; } } pc.set_spr_skill_time(noetime);
			 */

			if (pc.isDead()) { // 死亡
				return;
			}

			if (pc.isTeleport()) { // 傳送中（動畫／延遲 commit 窗口內再按技能）
				TeleportTrace.event(pc, "SKILL_DURING_TELEPORT_REJECTED", "phase", "packet_entry");
				// A competing skill must neither retire nor unlock the active teleport.
				return;
			}

			if (pc.isPrivateShop()) {// 商店村模式
				return;
			}

			if (pc.getInventory().getWeight240() >= 197) { // 重量過重
				pc.sendTeleportUnlockIfIdle();
				// 316 \f1你攜帶太多物品，因此無法使用法術。
				pc.sendPackets(new S_ServerMessage(316));
				return;
			}

			if (!pc.getMap().isUsableSkill()) {
				pc.sendTeleportUnlockIfIdle();
				// 563 \f1你無法在這個地方使用。
				pc.sendPackets(new S_ServerMessage(563));
				return;
			}

			ClientPacketStructure.validate(decrypt, client);

			// 先解析 skillId，供延遲／瞬移拒絕時解鎖與 trace
			final int row = this.readC();
			final int column = this.readC();
			final int skillId = (row << 3) + column + 1;

			// 技能延遲狀態
			if (pc.isSkillDelay()) {
				if (isTeleportSkillId(skillId)) {
					TeleportTrace.warn(pc,
							"技能共用 CD 中收到瞬移技能，僅在沒有進行中傳送時解鎖",
							"skillId", skillId);
					pc.sendTeleportUnlockIfIdle();
				}
				return;
			}

			if (skillId > 220) {
				return;
			}
			if (skillId < 0) {
				return;
			}
			final L1Skills skill = SkillsTable.get().getTemplate(skillId);
			if (skill == null) {
				return;
			}

			final int polyId = pc.getTempCharGfx();
			boolean isError = false;
			final L1PolyMorph poly = PolyTable.get().getTemplate(polyId);
			if ((poly != null) && !poly.canUseSkill()
					&& !L1PolyMorph.skipPolyTableSkillBlock(pc)) {
				isError = true;
			}
			if (pc.isParalyzed() && !isError) {
				isError = true;
			}
			if (pc.hasSkillEffect(SILENCE) && !isError) {
				isError = true;
			}
			if (pc.hasSkillEffect(AREA_OF_SILENCE) && !isError) {
				isError = true;
			}
			if (pc.hasSkillEffect(STATUS_POISON_SILENCE) && !isError) {
				isError = true;
			}
			if (pc.isParalyzedX() && !isError) {
				isError = true;
			}
			if (isError) {
				pc.sendTeleportUnlockIfIdle();
				pc.sendPackets(new S_ServerMessage(285));
				return;
			}

			// 內掛模式運作中僅限制攻擊性技能，治癒/Buff/傳送等可手動施放
			if (pc.isAiRunning()) {
				if ((skill.getType() & L1Skills.TYPE_ATTACK) != 0) {
					pc.sendPackets(new S_ServerMessage(166, "內掛執行中無法使用攻擊技能"));
					return;
				}
			}

			// 高寵模式運作中僅限制攻擊性技能，治癒/Buff 等可手動施放
			if (pc.isFollowRunning()) {
				if ((skill.getType() & L1Skills.TYPE_ATTACK) != 0) {
					pc.sendPackets(new S_ServerMessage(166, "高寵模式運作中無法使用攻擊技能"));
					return;
				}
			}

			// 技能合法判斷
			if (!pc.isSkillMastery(skillId)) {
				return;
			}

			// 檢查地圖使用權
			CheckUtil.isUserMap(pc);

			String charName = null;
			String requestedTrueTargetText = null;
			boolean hasRequestedTrueTargetText = false;
			int requestedSummonId = 0;
			boolean hasRequestedSummonId = false;
			L1Object requestedWorldTarget = null;
			L1Character requestedNowTarget = null;
			L1PcInstance requestedPetTarget = null;
			// String message = null;

			int targetId = 0;
			int targetX = 0;
			int targetY = 0;
			boolean checkLoc = false;
			if (decrypt.length > 4) {
				boolean isAttack = false;// 為攻擊型態技能
				switch (skillId) {
				case 43:// 加速術43
					try {
						targetId = readD();
						checkLoc = true;

					} catch (final Exception e) {
						if (e instanceof PacketFormatException malformed) throw malformed;
					}
					break;

				case 52:// 神聖疾走52
				case 54:// 強力加速術54
				case 101:// 行走加速101
				case 150:// 風之疾走150
					try {
						targetId = readD();

					} catch (final Exception e) {
						if (e instanceof PacketFormatException malformed) throw malformed;
					}
					break;

				case 186:// 血之渴望186
					try {
						targetId = readD();

					} catch (final Exception e) {
						if (e instanceof PacketFormatException malformed) throw malformed;
					}
					break;

				case 116:// 呼喚盟友
				case 118:// 援護盟友
					try {
						charName = readS();

					} catch (final Exception e) {
						if (e instanceof PacketFormatException malformed) throw malformed;
					}
					break;

				case 113:// 精準目標 1600
					try {
						targetId = readD();
						targetX = readH();
						targetY = readH();

						requestedTrueTargetText = readS();
						hasRequestedTrueTargetText = true;
						checkLoc = true;

					} catch (final Exception e) {
						if (e instanceof PacketFormatException malformed) throw malformed;
					}
					break;

				case 5:// 指定傳送 1100
				case 69:// 集體傳送術 1200
					try {
						readH(); // MapID
						targetId = readD(); // Bookmark ID

					} catch (final Exception e) {
						if (e instanceof PacketFormatException malformed) throw malformed;
					}
					break;

				case 58:// 火牢 1100
				case 63:// 治癒能量風暴 1100
					try {
						targetX = readH();
						targetY = readH();

					} catch (final Exception e) {
						if (e instanceof PacketFormatException malformed) throw malformed;
					}
					break;
				case 51:
					if ((pc.getInventory().checkEquipped(20284)) || (pc.getInventory().checkEquipped(120284))) {
						requestedSummonId = readD();
						hasRequestedSummonId = true;
					} else {
						// SUMMON_MONSTER is target="none". The client still sends a
						// DWORD here, but it is not a trusted World object target.
						// Consume it and leave targetId at 0 so L1SkillUse resolves the
						// caster as the target and executes the default summon branch.
						readD();
					}
					break;
				case 4:// 光箭 1100
				case 6:// 冰箭 1100
				case 7:// 風刃 1100
				case 10:// 寒冷戰慄 1100
				case 15:// 火箭 1300
				case 16:// 地獄之牙 1300
				case 17:// 極光雷電 1500
				case 18:// 起死回生術 1500
				case 22:// 寒冰氣息 1300
				case 25:// 燃燒的火球 1600
				case 28:// 吸血鬼之吻 1250
				case 30:// 巖牢 1600
				case 34:// 極道落雷 1250
				case 38:// 冰錐 1100
				case 45:// 地裂術 1100
				case 46:// 烈炎術 1650
				case 50:// 冰矛圍籬 1600
				case 65:// 雷霆風暴 3000
				case 74:// 流星雨 4000
				case 77:// 究極光裂術 6000
				case 108:// 會心一擊 1800
				case 132:// 三重矢 1250
				case 184:// 岩漿噴吐 3300
				case 187:// 屠宰者 3250
				case 192:// 奪命之雷 5150
				case 194:// 寒冰噴吐
				case 202:// 混亂 4000
				case 203:// 暴擊 3100
				case 207:// 心靈破壞 4050
				case 208:// 骷髏毀壞 3100
				case 213:// 武器破壞者 3100
					try {
						targetId = readD();
						targetX = readH();
						targetY = readH();
						isAttack = true;
						checkLoc = true;
						// _log.info("技能ID:" + skillId + "/"+targetId+"/"+targetX+"/"+targetY);

					} catch (final Exception e) {
						if (e instanceof PacketFormatException malformed) throw malformed;
					}
					break;

				case 1:// 初級治癒術 1300
				case 8:// 神聖武器 1500
				case 9:// 解毒術 1250
				case 11:// 毒咒 1650
				case 19:// 中級治癒術 1250
				case 20:// 闇盲咒術 1650
				case 23:// 能量感測 1100
				case 26:// 通暢氣脈術 1250
				case 27:// 壞物術 4200
				case 29:// 緩速術 1600
				case 33:// 木乃伊的詛咒 6200
				case 35:// 高級治癒術 1500
				case 36:// 迷魅術 1250
				case 37:// 聖潔之光 1250
				case 39:// 魔力奪取 1650
				case 40:// 黑闇之影 1650
				case 41:// 造屍術
				case 42:// 體魄強健術 1650
					// case 43:// 加速術 900
				case 44:// 魔法相消術 1450
				case 47:// 弱化術 1650
				case 48:// 祝福魔法武器 1250
				case 55:// 狂暴術 1250
				case 56:// 疾病術 1650
				case 57:// 全部治癒術 1650
				case 61:// 返生術
				case 64:// 魔法封印 1650
				case 66:// 沉睡之霧 2750
				case 67:// 變形術
				case 68:// 聖結界 2750
				case 71:// 藥水霜化術 1650
				case 75:// 終極返生術
				case 76:// 集體緩速術 4200
				case 79:// 靈魂昇華 1250
				case 87:// 衝擊之暈 9500
				case 103:// 暗黑盲咒 2700
				case 133:// 弱化屬性 1250
				case 145:// 釋放元素 1250
				case 148:// 火焰武器 1450
				case 149:// 風之神射 1450
				case 151:// 大地防護 1450
				case 152:// 地面障礙 1250
				case 153:// 魔法消除 1250
				case 157:// 大地屏障 1250
				case 158:// 生命之泉 1450
				case 160:// 水之防護 1450
				case 165:// 生命呼喚 1450
				case 167:// 風之枷鎖 1250
				case 170:// 水之元氣 1450
				case 173:// 污濁之水 2200
				case 174:// 精準射擊 4250
				case 188:// 恐懼無助 4250
				case 193:// 驚悚死神 5150
				case 204:// 幻覺：歐吉 3150
				case 206:// 專注 2050
				case 209:// 幻覺：巫妖 3100
				case 211:// 耐力 2050
				case 212:// 幻想 4050
				case 214:// 幻覺：鑽石高崙 3150
				case 216:// 洞察 2200
				case 217:// 恐慌 5150
				case 218:// 疼痛的歡愉 4050
				case 219:// 幻覺：化身 6100
					try {
						targetId = readD();
						checkLoc = true;
						// _log.info("技能ID:" + skillId + "/"+targetId+"/"+targetX+"/"+targetY);

					} catch (final Exception e) {
						if (e instanceof PacketFormatException malformed) throw malformed;
					}
					break;

				case 2:// 日光術 1300
				case 3:// 保護罩 1300
				case 12:// 擬似魔法武器 1500
				case 13:// 無所遁形術 1650
				case 14:// 負重強化 1250
				case 21:// 鎧甲護持 1500
				case 31:// 魔法屏障 1250
				case 32:// 冥想術 1250
				case 49:// 體力回復術 1650
					// case 51:// 召喚術 2200
					// case 52:// 神聖疾走 1250
				case 53:// 龍捲風 2500
					// case 54:// 強力加速術
				case 59:// 冰雪暴 2600
				case 60:// 隱身術 1250
				case 62:// 震裂術 2600
				case 70:// 火風暴 3000
				case 72:// 強力無所遁形術 1650
				case 73:// 創造魔法武器 1500
				case 78:// 絕對屏障 13000
				case 80:// 冰雪颶風 4000
					// case 88:// 增幅防禦 4500
				case 89:// 尖刺盔甲 6450
				case 90:// 堅固防護 4450
				case 97:// 暗隱術 11700
				case 98:// 附加劇毒 2750
				case 99:// 影之防護 2750
				case 100:// 提煉魔石 2000
					// case 101:// 行走加速 1750
				case 102:// 燃燒鬥志 4700
				case 104:// 毒性抵抗 1750
				case 105:// 雙重破壞 1750
				case 106:// 暗影閃避 1750
				case 107:// 暗影之牙 1700
				case 109:// 力量提升 1750
				case 110:// 敏捷提升 1750
				case 111:// 閃避提升 1750
				case 114:// 激勵士氣 1600
				case 115:// 鋼鐵士氣 1500
				case 117:// 衝擊士氣 1500
				case 129:// 魔法防禦 1300
				case 130:// 心靈轉換 1450
				case 131:// 世界樹的呼喚
				case 134:// 鏡反射 350
				case 137:// 淨化精神 1450
				case 138:// 屬性防禦 1450
				case 146:// 魂體轉換 2500
				case 147:// 單屬性防禦 1450
					// case 150:// 風之疾走 1450
				case 154:// 召喚屬性精靈 1400
				case 155:// 烈炎氣息 1400
				case 156:// 暴風之眼 1400
				case 159:// 大地的祝福 1450
				case 161:// 封印禁地 10300
				case 162:// 召喚強力屬性精靈 1450
				case 163:// 烈炎武器 1450
				case 164:// 生命的祝福 1650
				case 166:// 暴風神射 1450
				case 168:// 鋼鐵防護 1450
				case 169:// 體能激發 1450
				case 171:// 屬性之火 1450
				case 172:// 暴風疾走 1450
				case 175:// 烈焰之魂
				case 176:// 能量激發
				case 181:// 龍之護鎧 1300
				case 182:// 燃燒擊砍 3300
				case 183:// 護衛毀滅 2200
				case 185:// 覺醒：安塔瑞斯 1250
					// case 186:// 血之渴望
				case 189:// 衝擊之膚
				case 190:// 覺醒：法利昂 1250
				case 191:// 致命身軀 2350
				case 195:// 覺醒：巴拉卡斯 1250
				case 201:// 鏡像 1250
				case 205:// 立方：燃燒 6200
				case 210:// 立方：地裂 6250
				case 215:// 立方：衝擊 6200
				case 220:// 立方：和諧 6100
					try {
						targetId = readD();
						// _log.info("技能ID:" + skillId + "/"+targetId+"/"+targetX+"/"+targetY);

					} catch (final Exception e) {
						if (e instanceof PacketFormatException malformed) throw malformed;
					}
					break;

				case 88:// 增幅防禦4500
					try {
						if (pc.getInventory().getTypeEquipped(2, 7) >= 1) {
							targetId = readD();
						} else {
							return;
						}

					} catch (final Exception e) {
						if (e instanceof PacketFormatException malformed) throw malformed;
					}
					break;

				case 91:// 反擊屏障4450
					try {
						if (pc.getInventory().getTypeEquipped(1, 3) >= 1) {
							targetId = readD();
						} else {
							return;
						}
					} catch (final Exception e) {
						if (e instanceof PacketFormatException malformed) throw malformed;
					}
					break;

				default:
					try {
						targetId = readD();
						// _log.info("技能ID:" + skillId + "/"+targetId+"/"+targetX+"/"+targetY);

					} catch (final Exception e) {
						if (e instanceof PacketFormatException malformed) throw malformed;
						/*
						 * OutErrorMsg.put(this.getClass().getSimpleName(), "檢查 C_UseSkill 技能碼定位(核心管理者參考) SkillId: " + skillId, e);
						 */
					}
					break;
				}

				// target="none" 由 L1SkillUse 固定解析為施法者本人。部分客戶端仍會
				// 在封包尾端附帶 DWORD（隱身術會出現非 World objId 的值），不可
				// 將它當成世界物件驗證，否則會在效果、MP 與材料處理前直接略過。
				if (shouldResolveWorldObjectTarget(skill, targetId)) {
					final L1Object target = World.get().findObject(targetId);
					if (target == null) {
						SKIP_COUNT.incrementAndGet();
						return;
					}
					if (!PlayerTargetPositionFence.isAuthorized(pc, target, checkLoc)) {
						return;
					}
					requestedWorldTarget = target;
					if (isAttack && target instanceof L1Character) {
						requestedNowTarget = (L1Character) target;
						if (target instanceof L1PcInstance) {
							requestedPetTarget = (L1PcInstance) target;
						}
					}
				}
			}

			try {
				// 呼喚盟友/援護盟友
				if ((skillId == 116) || (skillId == 118)) {
					if (charName == null || charName.isEmpty()) {
						// 輸入名稱為空
						REJECT_COUNT.incrementAndGet();
						return;
					}

					final L1PcInstance target = World.get().getPlayer(charName);

					if (target == null) {
						// 73 \f1%0%d 不在線上。
						pc.sendPackets(new S_ServerMessage(73, charName));
						return;
					}

					// 無法攻擊/使用道具/技能/回城的狀態 XXX
					/*
					 * if (target.isParalyzedX()) { return; }
					 */

					if (pc.getClanid() != target.getClanid()) {
						// 您只能邀請您血盟中的成員。
						pc.sendPackets(new S_ServerMessage(414));
						return;
					}

					targetId = target.getId();
				}

				final int resolvedTargetId = targetId;
				final int resolvedTargetX = targetX;
				final int resolvedTargetY = targetY;
				final String acceptedTrueTargetText = requestedTrueTargetText;
				final boolean publishTrueTargetText = hasRequestedTrueTargetText;
				final int acceptedSummonId = requestedSummonId;
				final boolean publishSummonId = hasRequestedSummonId;
				final L1Object acceptedWorldTarget = requestedWorldTarget;
				final boolean requireTargetScreenVisibility = checkLoc;
				final L1Character acceptedNowTarget = requestedNowTarget;
				final L1PcInstance acceptedPetTarget = requestedPetTarget;
				PlayerActionLock.run(pc, () -> performSkill(client, pc, skill, skillId,
						resolvedTargetId, resolvedTargetX, resolvedTargetY,
						publishTrueTargetText, acceptedTrueTargetText,
						publishSummonId, acceptedSummonId,
						acceptedWorldTarget, requireTargetScreenVisibility,
						acceptedNowTarget, acceptedPetTarget));

			} catch (final Exception e) {
				if (e instanceof PacketFormatException malformed) throw malformed;
				ERROR_COUNT.incrementAndGet();
				_log.error(e.getLocalizedMessage(), e);
				/*
				 * OutErrorMsg.put(this.getClass().getSimpleName(), "檢查 C_UseSkill 程式執行位置(核心管理者參考) SkillId: " + skillId, e);
				 */
			}

		} catch (final Exception e) {
			if (e instanceof PacketFormatException malformed) throw malformed;
			ERROR_COUNT.incrementAndGet();
			_log.error(e.getLocalizedMessage(), e);

		} finally {
			this.over();
		}
	}

	private void performSkill(final ClientExecutor client, final L1PcInstance pc,
			final L1Skills skill, final int skillId, final int targetId,
			final int targetX, final int targetY,
			final boolean publishTrueTargetText, final String trueTargetText,
			final boolean publishSummonId, final int summonId,
			final L1Object worldTarget, final boolean requireTargetScreenVisibility,
			final L1Character nowTarget, final L1PcInstance petTarget) {
		if (client.getActiveChar() != pc || !client.isConnected()
				|| pc.isDead() || pc.isPrivateShop()) {
			return;
		}
		if (pc.isTeleport()) {
			TeleportTrace.event(pc, "SKILL_DURING_TELEPORT_REJECTED", "phase", "performSkill",
					"skillId", skillId);
			return;
		}
		final boolean attackSkill = (skill.getType() & L1Skills.TYPE_ATTACK) != 0;
		if (attackSkill && (pc.isAiRunning() || pc.isFollowRunning())) {
			return;
		}
		if (worldTarget != null
				&& (World.get().findObject(worldTarget.getId()) != worldTarget
						|| !PlayerTargetPositionFence.isAuthorized(pc, worldTarget,
								requireTargetScreenVisibility))) {
			return;
		}

		// L1SkillUse performs the full target/MP/material validation before it
		// acquires the shared attack/spell timeline. Holding PlayerActionLock here
		// serializes that final commit with Follow/Hunt movement and combat.
		final L1SkillUse skillUse = new L1SkillUse();
		skillUse.setAcceptedCastStatePublisher(() -> {
			if (publishTrueTargetText) {
				pc.publishTrueTargetText(trueTargetText);
			}
			if (publishSummonId) {
				pc.setSummonId(summonId);
			}
			// Parsing happens before the per-player action lock.  Do not publish a
			// detached object if the same world id was removed/replaced meanwhile.
			final boolean targetStillRegistered = nowTarget != null
					&& World.get().findObject(nowTarget.getId()) == nowTarget;
			if (targetStillRegistered) {
				pc.setNowTarget(nowTarget);
			}
			if (targetStillRegistered && petTarget == nowTarget) {
				pc.setPetTarget(petTarget);
			}
			if (skillId == 116) {
				pc.publishCallClanDestination(targetId);
			}
		});
		try {
			skillUse.handleCommands(pc, skillId, targetId, targetX, targetY,
					0, L1SkillUse.TYPE_NORMAL);
		} finally {
			if (publishTrueTargetText) {
				pc.clearTrueTargetText();
			}
		}
	}

	//@Override
	public String getType() {
		return this.getClass().getSimpleName();
	}

	public static String getMonitorLine() {
		return String.format(
				"CUseSkill start=%d reject=%d skip=%d error=%d skillInterruptAttack=%d skillInterruptMove=%d skillConflict=%d staleSkill=%d",
				START_COUNT.get(), REJECT_COUNT.get(), SKIP_COUNT.get(), ERROR_COUNT.get(),
				ClientActionTimeline.getSkillInterruptedAttackCount(),
				ClientActionTimeline.getSkillInterruptedMoveCount(),
				ClientActionTimeline.getSkillConflictRejectCount(),
				ClientActionTimeline.getStaleSkillRejectCount());
	}

	private static boolean isTeleportSkillId(final int skillId) {
		return skillId == 5 || skillId == 69 || skillId == 131;
	}

	static boolean shouldResolveWorldObjectTarget(final L1Skills skill, final int targetId) {
		return targetId != 0 && skill != null && !"none".equals(skill.getTarget());
	}

}
