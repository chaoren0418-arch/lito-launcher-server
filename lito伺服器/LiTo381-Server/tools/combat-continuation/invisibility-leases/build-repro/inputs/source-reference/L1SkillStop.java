package com.lineage.server.model.skill;

import static com.lineage.server.model.skill.L1SkillId.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.lineage.server.datatables.SkillsTable;
import com.lineage.server.model.L1Character;
import com.lineage.server.model.proc.ProcSkillEffectHelper;
import com.lineage.server.model.L1PolyMorph;
import com.lineage.server.model.Instance.L1MonsterInstance;
import com.lineage.server.model.Instance.L1NpcInstance;
import com.lineage.server.model.Instance.L1PcInstance;
import com.lineage.server.model.Instance.L1PetInstance;
import com.lineage.server.model.Instance.L1SummonInstance;
import com.lineage.server.model.skill.skillmode.SkillMode;
import com.lineage.server.model.talent.TalentDamageCalculator;
import com.lineage.server.serverpackets.S_IconConfig;
import com.lineage.server.serverpackets.S_Dexup;
import com.lineage.server.serverpackets.S_HPUpdate;
import com.lineage.server.serverpackets.S_Liquor;
import com.lineage.server.serverpackets.S_MagicEquipment;
import com.lineage.server.serverpackets.S_MPUpdate;
import com.lineage.server.serverpackets.S_OwnCharAttrDef;
import com.lineage.server.serverpackets.S_OwnCharStatus;
import com.lineage.server.serverpackets.S_PacketBox;
import com.lineage.server.serverpackets.S_PacketBoxCooking;
import com.lineage.server.serverpackets.S_PacketBoxIconAura;
import com.lineage.server.serverpackets.S_PacketBoxWaterLife;
import com.lineage.server.serverpackets.S_PacketBoxWisdomPotion;
import com.lineage.server.serverpackets.S_Paralysis;
import com.lineage.server.serverpackets.S_Poison;
import com.lineage.server.serverpackets.S_SPMR;
import com.lineage.server.serverpackets.S_ServerMessage;
import com.lineage.server.serverpackets.S_SkillBrave;
import com.lineage.server.serverpackets.S_SkillHaste;
import com.lineage.server.serverpackets.S_SkillIconBlessOfEva;
import com.lineage.server.serverpackets.S_SkillIconShield;
import com.lineage.server.serverpackets.S_Strup;
import com.lineage.server.templates.L1Skills;

/**
 * 技能停止
 * 
 * @author dexc
 *
 */
public class L1SkillStop {

	private static final Logger _log = LogManager.getLogger(L1SkillStop.class);

	public static void stopSkill(final L1Character cha, final int skillId) {
		try {
			// 資料驅動：若該 skill 在 etcitem_buff藥水設置 有設定，依 group_check 發訊息並重算 Buff，不進硬編碼
			// switch
			final com.lineage.server.model.item.L1ItemBuffConfig config = com.lineage.server.datatables.EtcItemBuffConfigLoader
					.get().getConfigByBuffId(skillId);
			if (config != null && cha instanceof L1PcInstance) {
				final L1PcInstance pc = (L1PcInstance) cha;
				if (config.getEndMsgStr() != null && !config.getEndMsgStr().trim().isEmpty()) {
					pc.sendPackets(new S_ServerMessage(config.getEndMsgStr()));
				}
				if (config.getStatusIcon() > 0) {
					if (config.getStatusIcon() >= 256) {
						pc.sendPackets(new S_IconConfig(S_IconConfig.SKILL_ICON, config.getStatusIcon(), 0, true));
					} else {
						pc.sendPackets(new S_PacketBoxCooking(pc, config.getStatusIcon(), 0));
					}
				}
				pc.updateBuffStatus();
				// 覺醒若與 etcitem_buff 的 buff_id 重疊，仍須執行 SkillMode.stop（還原外觀／_awakeSkillId）
				if (skillId != AWAKEN_ANTHARAS && skillId != AWAKEN_FAFURION && skillId != AWAKEN_VALAKAS) {
					return;
				}
			}

			final SkillMode mode = L1SkillMode.get().getSkill(skillId);
			if (mode != null) {
				mode.stop(cha);

			} else {
				switch (skillId) {
					/*
					 * case IMMUNE_TO_HARM:
					 * case BOUNCE_ATTACK:
					 * case SHIELD:
					 */
					case LIGHT: // 光
						if (cha instanceof L1PcInstance) {
							if (!cha.isInvisble()) {
								final L1PcInstance pc = (L1PcInstance) cha;
								pc.turnOnOffLight();
							}
						}
						break;

					case GLOWING_AURA: // 光輝靈氣
						// [Phase 5] 移除直接戰鬥屬性修改，改用 Buff 系統
						// cha.addHitup(-5); // 已遷移至 L1PcBuffAbilityCalc
						// cha.addBowHitup(-5); // 已遷移至 L1PcBuffAbilityCalc
						cha.addMr(-20);
						if (cha instanceof L1PcInstance) {
							final L1PcInstance pc = (L1PcInstance) cha;
							pc.updateBuffStatus(); // 使用新的 Buff 系統
							pc.sendPackets(new S_SPMR(pc));
							IndependentSkillIcon.sendStop(pc, skillId);
						}
						break;

					case SHINING_AURA: // 鋼鐵士氣
						// [Phase 5] 移除直接戰鬥屬性修改，改用 Buff 系統
						// cha.addAc(8); // 已遷移至 L1PcBuffAbilityCalc
						if (cha instanceof L1PcInstance) {
							final L1PcInstance pc = (L1PcInstance) cha;
							pc.updateBuffStatus(); // 使用新的 Buff 系統
							IndependentSkillIcon.sendStop(pc, skillId);
						}
						break;

					case BRAVE_AURA: // 衝擊士氣
						// [Phase 5] 移除直接戰鬥屬性修改，改用 Buff 系統
						// cha.addDmgup(-5); // 已遷移至 L1PcBuffAbilityCalc
						if (cha instanceof L1PcInstance) {
							final L1PcInstance pc = (L1PcInstance) cha;
							pc.updateBuffStatus(); // 使用新的 Buff 系統
							pc.sendPackets(new S_PacketBoxIconAura(116, 0));
						}
						break;

					case SHIELD: // 盾牌
						// [Phase 5] 移除直接戰鬥屬性修改，改用 Buff 系統
						// cha.addAc(2); // 已遷移至 L1PcBuffAbilityCalc
						if (cha instanceof L1PcInstance) {
							final L1PcInstance pc = (L1PcInstance) cha;
							pc.updateBuffStatus(); // 使用新的 Buff 系統
							IndependentSkillIcon.sendStop(pc, skillId);
						}
						break;

					case BLIND_HIDING: // 盲目隱藏
						if (cha instanceof L1PcInstance) {
							final L1PcInstance pc = (L1PcInstance) cha;
							pc.delBlindHiding();
						}
						break;

					case SHADOW_ARMOR: // 暗影盔甲
						// [Phase 5] 移除直接戰鬥屬性修改，改用 Buff 系統
						// cha.addAc(3); // 已遷移至 L1PcBuffAbilityCalc
						if (cha instanceof L1PcInstance) {
							final L1PcInstance pc = (L1PcInstance) cha;
							pc.updateBuffStatus(); // 使用新的 Buff 系統
							IndependentSkillIcon.sendStop(pc, skillId);
						}
						break;

					case DRESS_DEXTERITY: // 服飾靈敏
						// [Phase 4] 移除直接屬性修改，改用 Buff 系統
						// cha.addDex((byte) -2); // 已遷移至 L1PcBuffAbilityCalc
						if (cha instanceof L1PcInstance) {
							final L1PcInstance pc = (L1PcInstance) cha;
							pc.updateBuffStatus(); // 使用新的 Buff 系統
							IndependentSkillIcon.sendStop(pc, skillId);
						}
						break;

					case DRESS_MIGHTY: // 服飾力量
						// [Phase 4] 移除直接屬性修改，改用 Buff 系統
						// cha.addStr((byte) -2); // 已遷移至 L1PcBuffAbilityCalc
						if (cha instanceof L1PcInstance) {
							final L1PcInstance pc = (L1PcInstance) cha;
							pc.updateBuffStatus(); // 使用新的 Buff 系統
							IndependentSkillIcon.sendStop(pc, skillId);
						}
						break;

					case HOLY_WEAPON: // 神聖武器（玩家 Buff，與裝備解耦）
						// [Phase 5] 移除直接戰鬥屬性修改，改用 Buff 系統
						// cha.addDmgup(-1); // 已遷移至 L1PcBuffAbilityCalc
						// cha.addHitup(-1); // 已遷移至 L1PcBuffAbilityCalc
						if (cha instanceof L1PcInstance) {
							final L1PcInstance pc = (L1PcInstance) cha;
							pc.updateBuffStatus(); // 使用新的 Buff 系統
							IndependentSkillIcon.sendStop(pc, skillId);
						}
						break;

					case SHADOW_FANG: // 暗影獠牙
						// [Phase 5] 移除直接戰鬥屬性修改，改用 Buff 系統
						// cha.addDmgup(-5); // 已遷移至 L1PcBuffAbilityCalc
						if (cha instanceof L1PcInstance) {
							((L1PcInstance) cha).updateBuffStatus(); // 使用新的 Buff 系統
						}
						break;

					case ENCHANT_WEAPON: // 附魔武器
						// [Phase 5] 移除直接戰鬥屬性修改，改用 Buff 系統
						// cha.addDmgup(-2); // 已遷移至 L1PcBuffAbilityCalc
						if (cha instanceof L1PcInstance) {
							final L1PcInstance pc = (L1PcInstance) cha;
							pc.updateBuffStatus(); // 使用新的 Buff 系統
							IndependentSkillIcon.sendStop(pc, skillId);
						}
						break;

					case BLESSED_ARMOR: // 祝福盔甲
						// [Phase 5] 移除直接戰鬥屬性修改，改用 Buff 系統
						// cha.addAc(3); // 已遷移至 L1PcBuffAbilityCalc
						if (cha instanceof L1PcInstance) {
							((L1PcInstance) cha).updateBuffStatus(); // 使用新的 Buff 系統
						}
						break;

					case EARTH_BLESS: // 大地祝福
						// [Phase 5] 移除直接戰鬥屬性修改，改用 Buff 系統
						// cha.addAc(7); // 已遷移至 L1PcBuffAbilityCalc
						if (cha instanceof L1PcInstance) {
							final L1PcInstance pc = (L1PcInstance) cha;
							pc.updateBuffStatus(); // 使用新的 Buff 系統
							IndependentSkillIcon.sendStop(pc, skillId);
						}
						break;

					case RESIST_MAGIC: // 魔法抵抗
						cha.addMr(-10);
						if (cha instanceof L1PcInstance) {
							final L1PcInstance pc = (L1PcInstance) cha;
							pc.sendPackets(new S_SPMR(pc));
						}
						break;

					case CLEAR_MIND: // 清晰心智
						// [Phase 4] 移除直接屬性修改，改用 Buff 系統
						// cha.addWis((byte) -3); // 已遷移至 L1PcBuffAbilityCalc
						if (cha instanceof L1PcInstance) {
							final L1PcInstance pc = (L1PcInstance) cha;
							pc.updateBuffStatus(); // 使用新的 Buff 系統
							pc.resetBaseMr();
						}
						break;

					case RESIST_ELEMENTAL: // 元素抵抗
						cha.addWind(-10);
						cha.addWater(-10);
						cha.addFire(-10);
						cha.addEarth(-10);
						if (cha instanceof L1PcInstance) {
							final L1PcInstance pc = (L1PcInstance) cha;
							pc.sendPackets(new S_OwnCharAttrDef(pc));
						}
						break;

					case ELEMENTAL_PROTECTION: // 元素保護
						if (cha instanceof L1PcInstance) {
							final L1PcInstance pc = (L1PcInstance) cha;
							// 與施放時一致：位元遮罩，擁有幾種精靈屬性就各扣回 50
							final int attr = pc.getElfAttr();
							if ((attr & 1) != 0) {
								cha.addEarth(-50);
							}
							if ((attr & 2) != 0) {
								cha.addFire(-50);
							}
							if ((attr & 4) != 0) {
								cha.addWater(-50);
							}
							if ((attr & 8) != 0) {
								cha.addWind(-50);
							}
							pc.sendPackets(new S_OwnCharAttrDef(pc));
						}
						break;

					case WATER_LIFE: // 水之元氣
						if (cha instanceof L1PcInstance) {
							final L1PcInstance pc = (L1PcInstance) cha;
							pc.sendPackets(new S_PacketBoxWaterLife());
						}
						break;

					/*
					 * case ELEMENTAL_FALL_DOWN: // 元素衰弱
					 * if (cha instanceof L1PcInstance) {
					 * final L1PcInstance pc = (L1PcInstance) cha;
					 * final int attr = pc.getAddAttrKind();
					 * final int i = 50;
					 * switch (attr) {
					 * case 1:
					 * pc.addEarth(i);
					 * break;
					 * case 2:
					 * pc.addFire(i);
					 * break;
					 * case 4:
					 * pc.addWater(i);
					 * break;
					 * case 8:
					 * pc.addWind(i);
					 * break;
					 * default:
					 * break;
					 * }
					 * pc.setAddAttrKind(0);
					 * pc.sendPackets(new S_OwnCharAttrDef(pc));
					 * } else if (cha instanceof L1NpcInstance) {
					 * final L1NpcInstance npc = (L1NpcInstance) cha;
					 * final int attr = npc.getAddAttrKind();
					 * final int i = 50;
					 * switch (attr) {
					 * case 1:
					 * npc.addEarth(i);
					 * break;
					 * case 2:
					 * npc.addFire(i);
					 * break;
					 * case 4:
					 * npc.addWater(i);
					 * break;
					 * case 8:
					 * npc.addWind(i);
					 * break;
					 * default:
					 * break;
					 * }
					 * npc.setAddAttrKind(0);
					 * }
					 * break;
					 */

					case IRON_SKIN: // 鐵皮
						// [Phase 5] 移除直接戰鬥屬性修改，改用 Buff 系統
						// cha.addAc(10); // 已遷移至 L1PcBuffAbilityCalc
						if (cha instanceof L1PcInstance) {
							final L1PcInstance pc = (L1PcInstance) cha;
							pc.updateBuffStatus(); // 使用新的 Buff 系統
							IndependentSkillIcon.sendStop(pc, skillId);
						}
						break;

					case EARTH_SKIN: // 大地皮膚
						// [Phase 5] 移除直接戰鬥屬性修改，改用 Buff 系統
						// cha.addAc(6); // 已遷移至 L1PcBuffAbilityCalc
						if (cha instanceof L1PcInstance) {
							final L1PcInstance pc = (L1PcInstance) cha;
							pc.updateBuffStatus(); // 使用新的 Buff 系統
							IndependentSkillIcon.sendStop(pc, skillId);
						}
						break;

					case PHYSICAL_ENCHANT_STR: // 物理附魔：力量
						// [Phase 2] 移除直接屬性修改，改用 Buff 系統
						// cha.addStr((byte) -5); // 已遷移至 L1PcBuffAbilityCalc
						if (cha instanceof L1PcInstance) {
							final L1PcInstance pc = (L1PcInstance) cha;
							pc.updateBuffStatus(); // 使用新的 Buff 系統
							IndependentSkillIcon.sendStop(pc, skillId);
						}
						break;

					case PHYSICAL_ENCHANT_DEX:
						// [Phase 4] 移除直接屬性修改，改用 Buff 系統
						// cha.addDex((byte) -5); // 已遷移至 L1PcBuffAbilityCalc

						if (cha instanceof L1PcInstance) {
							final L1PcInstance pc = (L1PcInstance) cha;
							pc.updateBuffStatus(); // <--- 加入這行，讓系統重算 (因為技能ID已經從清單移除，重算後自然會扣除加成)
							IndependentSkillIcon.sendStop(pc, skillId);
						}
						break;

					case FIRE_WEAPON: // 火焰武器
						// [Phase 5] 移除直接戰鬥屬性修改，改用 Buff 系統
						// cha.addDmgup(-4); // 已遷移至 L1PcBuffAbilityCalc
						if (cha instanceof L1PcInstance) {
							final L1PcInstance pc = (L1PcInstance) cha;
							pc.updateBuffStatus(); // 使用新的 Buff 系統
							IndependentSkillIcon.sendStop(pc, skillId);
						}
						break;

					case FIRE_BLESS: // 火焰祝福
						// [Phase 5] 移除直接戰鬥屬性修改，改用 Buff 系統
						// cha.addDmgup(-4); // 已遷移至 L1PcBuffAbilityCalc
						if (cha instanceof L1PcInstance) {
							final L1PcInstance pc = (L1PcInstance) cha;
							pc.updateBuffStatus(); // 使用新的 Buff 系統
							IndependentSkillIcon.sendStop(pc, skillId);
						}
						break;

					case BURNING_WEAPON: // 燃燒武器
						// [Phase 5] 移除直接戰鬥屬性修改，改用 Buff 系統
						// cha.addDmgup(-6); // 已遷移至 L1PcBuffAbilityCalc
						// cha.addHitup(-3); // 已遷移至 L1PcBuffAbilityCalc
						if (cha instanceof L1PcInstance) {
							final L1PcInstance pc = (L1PcInstance) cha;
							pc.updateBuffStatus(); // 使用新的 Buff 系統
							IndependentSkillIcon.sendStop(pc, skillId);
						}
						break;

					case BLESS_WEAPON: // 祝福武器
						// [Phase 5] 移除直接戰鬥屬性修改，改用 Buff 系統
						// cha.addDmgup(-2); // 已遷移至 L1PcBuffAbilityCalc
						// cha.addHitup(-2); // 已遷移至 L1PcBuffAbilityCalc
						// cha.addBowHitup(-2); // 已遷移至 L1PcBuffAbilityCalc
						if (cha instanceof L1PcInstance) {
							final L1PcInstance pc = (L1PcInstance) cha;
							pc.updateBuffStatus(); // 使用新的 Buff 系統
							IndependentSkillIcon.sendStop(pc, skillId);
						}
						break;

					case WIND_SHOT: // 風之箭
						// [Phase 5] 移除直接戰鬥屬性修改，改用 Buff 系統
						// cha.addBowHitup(-6); // 已遷移至 L1PcBuffAbilityCalc
						if (cha instanceof L1PcInstance) {
							final L1PcInstance pc = (L1PcInstance) cha;
							pc.updateBuffStatus(); // 使用新的 Buff 系統
							IndependentSkillIcon.sendStop(pc, skillId);
						}
						break;

					case STORM_EYE: // 暴風之眼
						// [Phase 5] 移除直接戰鬥屬性修改，改用 Buff 系統
						// cha.addBowHitup(-2); // 已遷移至 L1PcBuffAbilityCalc
						// cha.addBowDmgup(-3); // 已遷移至 L1PcBuffAbilityCalc
						if (cha instanceof L1PcInstance) {
							final L1PcInstance pc = (L1PcInstance) cha;
							pc.updateBuffStatus(); // 使用新的 Buff 系統
							IndependentSkillIcon.sendStop(pc, skillId);
						}
						break;

					case STORM_SHOT: // 暴風射擊
						// [Phase 5] 移除直接戰鬥屬性修改，改用 Buff 系統
						// cha.addBowDmgup(-5); // 已遷移至 L1PcBuffAbilityCalc
						// cha.addBowHitup(1); // 已遷移至 L1PcBuffAbilityCalc
						if (cha instanceof L1PcInstance) {
							final L1PcInstance pc = (L1PcInstance) cha;
							pc.updateBuffStatus(); // 使用新的 Buff 系統
							IndependentSkillIcon.sendStop(pc, skillId);
						}
						break;

					case BERSERKERS: // 狂暴術
						// [Phase 5] 移除直接戰鬥屬性修改，改用 Buff 系統
						// cha.addAc(-10); // 已遷移至 L1PcBuffAbilityCalc
						// cha.addDmgup(-5); // 已遷移至 L1PcBuffAbilityCalc
						// cha.addHitup(-2); // 已遷移至 L1PcBuffAbilityCalc
						if (cha instanceof L1PcInstance) {
							final L1PcInstance pc = (L1PcInstance) cha;
							pc.updateBuffStatus(); // 使用新的 Buff 系統
						}
						break;

					case SHAPE_CHANGE: // 變形
						L1PolyMorph.undoPoly(cha);
						break;

					/*
					 * case ADVANCE_SPIRIT: // 高級精神
					 * if (cha instanceof L1PcInstance) {
					 * final L1PcInstance pc = (L1PcInstance) cha;
					 * pc.addMaxHp(-pc.getAdvenHp());
					 * pc.addMaxMp(-pc.getAdvenMp());
					 * pc.setAdvenHp(0);
					 * pc.setAdvenMp(0);
					 * pc.sendPackets(new S_HPUpdate(pc.getCurrentHp(), pc.getMaxHp()));
					 * if (pc.isInParty()) { // 隊伍中
					 * pc.getParty().updateMiniHP(pc);
					 * }
					 * pc.sendPackets(new S_MPUpdate(pc.getCurrentMp(), pc.getMaxMp()));
					 * }
					 * break;
					 */

					// case HASTE:
					case GREATER_HASTE: // 加速、高等加速
						cha.setMoveSpeed(0);
						if (cha instanceof L1PcInstance) {
							final L1PcInstance pc = (L1PcInstance) cha;
							pc.sendPacketsAll(new S_SkillHaste(pc.getId(), 0, 0));
						} else {
							cha.broadcastPacketAll(new S_SkillHaste(cha.getId(), 0, 0));
						}
						break;

					case HOLY_WALK:
					case MOVING_ACCELERATION:
					case WIND_WALK:
						// case BLOODLUST: // 聖潔步伐、移動加速、風之步、嗜血
						cha.setBraveSpeed(0);
						if (cha instanceof L1PcInstance) {
							final L1PcInstance pc = (L1PcInstance) cha;
							pc.sendPacketsAll(new S_SkillBrave(pc.getId(), 0, 0));
						} else {
							cha.broadcastPacketAll(new S_SkillBrave(cha.getId(), 0, 0));
						}
						break;

					case ILLUSION_OGRE: // 幻象：食人魔
						if (cha instanceof L1PcInstance) {
							final L1PcInstance pc = (L1PcInstance) cha;
							// [Phase 5] 移除直接戰鬥屬性修改，改用 Buff 系統
							// pc.addDmgup(-4); // 已遷移至 L1PcBuffAbilityCalc
							// pc.addHitup(-4); // 已遷移至 L1PcBuffAbilityCalc
							pc.updateBuffStatus(); // 使用新的 Buff 系統
						}
						break;

					/*
					 * case ILLUSION_LICH: // 幻象：巫妖
					 * if (cha instanceof L1PcInstance) {
					 * final L1PcInstance pc = (L1PcInstance) cha;
					 * pc.addSp(-2);
					 * pc.sendPackets(new S_SPMR(pc));
					 * }
					 * break;
					 */

					case ILLUSION_DIA_GOLEM: // 幻象：鑽石魔像
						if (cha instanceof L1PcInstance) {
							final L1PcInstance pc = (L1PcInstance) cha;
							// [Phase 5] 移除直接戰鬥屬性修改，改用 Buff 系統
							// pc.addAc(20); // 已遷移至 L1PcBuffAbilityCalc
							pc.updateBuffStatus(); // 使用新的 Buff 系統
						}
						break;

					/*
					 * case ILLUSION_AVATAR: // 幻象：化身
					 * if (cha instanceof L1PcInstance) {
					 * final L1PcInstance pc = (L1PcInstance) cha;
					 * pc.addDmgup(-10);
					 * }
					 * break;
					 */

					/*
					 * case INSIGHT: // 洞察
					 * if (cha instanceof L1PcInstance) {
					 * final L1PcInstance pc = (L1PcInstance) cha;
					 * pc.addStr((byte) -1);
					 * pc.addCon((byte) -1);
					 * pc.addDex((byte) -1);
					 * pc.addWis((byte) -1);
					 * pc.addInt((byte) -1);
					 * }
					 * break;
					 */

					/*
					 * case CURSE_BLIND:
					 * case DARKNESS:
					 * if (cha instanceof L1PcInstance) {
					 * final L1PcInstance pc = (L1PcInstance) cha;
					 * pc.sendPackets(new S_CurseBlind(0));
					 * }
					 * break;
					 */

					/*
					 * case CURSE_PARALYZE: // 詛咒麻痺
					 * if (cha instanceof L1PcInstance) {
					 * final L1PcInstance pc = (L1PcInstance) cha;
					 * pc.sendPacketsAll(new S_Poison(pc.getId(), 0));
					 * pc.sendPackets(new S_Paralysis(S_Paralysis.TYPE_PARALYSIS, false));
					 * }
					 * break;
					 */

					case WEAKNESS: // 衰弱
						if (cha instanceof L1PcInstance) {
							final L1PcInstance pc = (L1PcInstance) cha;
							// [Phase 5] 移除直接戰鬥屬性修改，改用 Buff 系統
							// pc.addDmgup(5); // 已遷移至 L1PcBuffAbilityCalc
							// pc.addHitup(1); // 已遷移至 L1PcBuffAbilityCalc
							pc.updateBuffStatus(); // 使用新的 Buff 系統
							ProcSkillEffectHelper.syncPcSkillIcons(pc);
						}
						break;

					case DISEASE: // 疾病
						if (cha instanceof L1PcInstance) {
							final L1PcInstance pc = (L1PcInstance) cha;
							// [Phase 5] 移除直接戰鬥屬性修改，改用 Buff 系統
							// pc.addDmgup(6); // 已遷移至 L1PcBuffAbilityCalc
							// pc.addAc(-12); // 已遷移至 L1PcBuffAbilityCalc
							pc.updateBuffStatus(); // 使用新的 Buff 系統
							ProcSkillEffectHelper.syncPcSkillIcons(pc);
						}
						break;

					case ICE_LANCE: // 冰槍
					case FREEZING_BLIZZARD: // 冰凍暴風雪
					case FREEZING_BREATH: // 冰凍吐息
						if (cha instanceof L1PcInstance) {
							final L1PcInstance pc = (L1PcInstance) cha;
							pc.sendPacketsAll(new S_Poison(pc.getId(), 0));

							pc.sendPackets(new S_Paralysis(S_Paralysis.TYPE_FREEZE, false));
						} else if ((cha instanceof L1MonsterInstance)
								|| (cha instanceof L1SummonInstance)
								|| (cha instanceof L1PetInstance)) {
							final L1NpcInstance npc = (L1NpcInstance) cha;
							npc.broadcastPacketAll(new S_Poison(npc.getId(), 0));
							npc.setParalyzed(false);
						}
						break;

					case EARTH_BIND: // 大地束縛
						if (cha instanceof L1PcInstance) {
							final L1PcInstance pc = (L1PcInstance) cha;
							pc.sendPacketsAll(new S_Poison(pc.getId(), 0));

							pc.sendPackets(new S_Paralysis(S_Paralysis.TYPE_FREEZE, false));
						} else if ((cha instanceof L1MonsterInstance)
								|| (cha instanceof L1SummonInstance)
								|| (cha instanceof L1PetInstance)) {
							final L1NpcInstance npc = (L1NpcInstance) cha;
							npc.broadcastPacketAll(new S_Poison(npc.getId(), 0));
							npc.setParalyzed(false);
						}
						break;

					/*
					 * case SHOCK_STUN: // 衝擊暈眩
					 * if (cha instanceof L1PcInstance) {
					 * final L1PcInstance pc = (L1PcInstance) cha;
					 * pc.sendPackets(new S_Paralysis(S_Paralysis.TYPE_STUN, false));
					 * } else if ((cha instanceof L1MonsterInstance)
					 * || (cha instanceof L1SummonInstance)
					 * || (cha instanceof L1PetInstance)) {
					 * final L1NpcInstance npc = (L1NpcInstance) cha;
					 * npc.setParalyzed(false);
					 * }
					 * break;
					 */

					case TALENT_STUN: // 天賦暈眩（非 SkillsTable 技能，無 SkillMode；須對齊 TalentDamageCalculator.applyTalentStun）
						// 與技能計時同步移除星星特效（含提前解除；deleteMe 可與排程刪除並行，具 destroyed 防重入）
						TalentDamageCalculator.removeTalentStunEffectVisual(cha);
						if (cha instanceof L1PcInstance) {
							final L1PcInstance pc = (L1PcInstance) cha;
							pc.sendPackets(new S_Paralysis(S_Paralysis.TYPE_STUN, false));
						} else if (cha instanceof L1NpcInstance) {
							((L1NpcInstance) cha).setParalyzed(false);
						}
						break;

					case FOG_OF_SLEEPING: // 沉睡之霧
						cha.setSleeped(false);
						if (cha instanceof L1PcInstance) {
							final L1PcInstance pc = (L1PcInstance) cha;
							pc.sendPackets(new S_Paralysis(S_Paralysis.TYPE_SLEEP, false));
							pc.sendPackets(new S_OwnCharStatus(pc));
						}
						break;

					case ABSOLUTE_BARRIER: // 絕對屏障
						if (cha instanceof L1PcInstance) {
							final L1PcInstance pc = (L1PcInstance) cha;
							pc.startHpRegeneration();
							pc.startMpRegeneration();
						}
						break;

					/*
					 * case WIND_SHACKLE: // 風之枷鎖
					 * if (cha instanceof L1PcInstance) {
					 * final L1PcInstance pc = (L1PcInstance) cha;
					 * pc.sendPackets(new S_PacketBoxWindShackle(pc.getId(), 0));
					 * }
					 * break;
					 */

					case SLOW:
					case ENTANGLE:
					case MASS_SLOW: // 緩速術/集體緩速術/地面障礙
						if (cha instanceof L1PcInstance) {
							final L1PcInstance pc = (L1PcInstance) cha;
							pc.sendPacketsAll(new S_SkillHaste(pc.getId(), 0, 0));
						}
						cha.setMoveSpeed(0);
						break;

					/*
					 * case STATUS_FREEZE: // Freeze
					 * if (cha instanceof L1PcInstance) {
					 * final L1PcInstance pc = (L1PcInstance) cha;
					 * pc.sendPackets(new S_Paralysis(S_Paralysis.TYPE_BIND, false));
					 * 
					 * } else if ((cha instanceof L1MonsterInstance)
					 * || (cha instanceof L1SummonInstance)
					 * || (cha instanceof L1PetInstance)) {
					 * final L1NpcInstance npc = (L1NpcInstance) cha;
					 * npc.setParalyzed(false);
					 * }
					 * break;
					 */

					case GUARD_BRAKE: // 破甲
						if (cha instanceof L1PcInstance) {
							final L1PcInstance pc = (L1PcInstance) cha;
							// [Phase 5] 移除直接戰鬥屬性修改，改用 Buff 系統
							// pc.addAc(-15); // 已遷移至 L1PcBuffAbilityCalc
							pc.updateBuffStatus(); // 使用新的 Buff 系統
						}
						break;

					case HORROR_OF_DEATH: // 死亡恐懼
						if (cha instanceof L1PcInstance) {
							final L1PcInstance pc = (L1PcInstance) cha;
							// [Phase 4] 移除直接屬性修改，改用 Buff 系統
							// pc.addStr(5); // 已遷移至 L1PcBuffAbilityCalc
							// pc.addInt(5); // 已遷移至 L1PcBuffAbilityCalc
							pc.updateBuffStatus(); // 使用新的 Buff 系統
						}
						break;

					case STATUS_CUBE_IGNITION_TO_ALLY: // 方塊[點火]：友方
						cha.addFire(-30);
						if (cha instanceof L1PcInstance) {
							final L1PcInstance pc = (L1PcInstance) cha;
							pc.sendPackets(new S_OwnCharAttrDef(pc));
						}
						break;

					case STATUS_CUBE_QUAKE_TO_ALLY: // 方塊[地震]：友方
						cha.addEarth(-30);
						if (cha instanceof L1PcInstance) {
							final L1PcInstance pc = (L1PcInstance) cha;
							pc.sendPackets(new S_OwnCharAttrDef(pc));
						}
						break;

					case STATUS_CUBE_SHOCK_TO_ALLY: // 方塊[衝擊]：友方
						cha.addWind(-30);
						if (cha instanceof L1PcInstance) {
							final L1PcInstance pc = (L1PcInstance) cha;
							pc.sendPackets(new S_OwnCharAttrDef(pc));
						}
						break;

					case STATUS_CUBE_IGNITION_TO_ENEMY: // 方塊[點火]：敵方
					case STATUS_CUBE_QUAKE_TO_ENEMY: // 方塊[地震]：敵方
					case STATUS_CUBE_SHOCK_TO_ENEMY: // 方塊[衝擊]：敵方
					case STATUS_MR_REDUCTION_BY_CUBE_SHOCK: // 方塊[衝擊]造成的 MR 減少
					case STATUS_CUBE_BALANCE: // 方塊[平衡]
						break;

					case STATUS_BRAVE: // 勇敢藥水效果
						if (cha instanceof L1PcInstance) {
							final L1PcInstance pc = (L1PcInstance) cha;
							pc.sendPacketsAll(new S_SkillBrave(pc.getId(), 0, 0));
						}
						cha.setBraveSpeed(0);
						break;

					case STATUS_BRAVE3: // 巧克力蛋糕
						if (cha instanceof L1PcInstance) {
							final L1PcInstance pc = (L1PcInstance) cha;
							pc.sendPacketsAll(new S_Liquor(pc.getId(), 0x00));
						}
						break;

					case STATUS_ELFBRAVE: // 精靈餅乾效果
						if (cha instanceof L1PcInstance) {
							final L1PcInstance pc = (L1PcInstance) cha;
							pc.sendPacketsAll(new S_SkillBrave(pc.getId(), 0, 0));
						}
						cha.setBraveSpeed(0);
						break;

					case STATUS_RIBRAVE: // 生命之樹果實效果
						if (cha instanceof L1PcInstance) {
							final L1PcInstance pc = (L1PcInstance) cha;
							pc.setBraveSpeed(0);
							pc.sendPacketsAll(new S_SkillHaste(pc.getId(), 0, 0));
							// XXX 生命之樹果實圖標的清除方式尚不明

						} else {
							cha.setBraveSpeed(0);
						}
						break;

					case STATUS_HASTE: // 加速藥水效果
						if (cha instanceof L1PcInstance) {
							final L1PcInstance pc = (L1PcInstance) cha;
							pc.sendPacketsAll(new S_SkillHaste(pc.getId(), 0, 0));
						}
						cha.setMoveSpeed(0);
						break;

					case STATUS_BLUE_POTION: // 魔力回復藥水效果
						break;

					case STATUS_UNDERWATER_BREATH: // 伊娃的祝福藥水效果
						if (cha instanceof L1PcInstance) {
							final L1PcInstance pc = (L1PcInstance) cha;
							pc.sendPackets(new S_SkillIconBlessOfEva(pc.getId(), 0));
						}
						break;

					case STATUS_WISDOM_POTION: // 慎重藥水效果
						if (cha instanceof L1PcInstance) {
							final L1PcInstance pc = (L1PcInstance) cha;
							cha.addSp(-2);
							pc.sendPackets(new S_PacketBoxWisdomPotion(0));
						}
						break;

					case STATUS_CHAT_PROHIBITED: // 禁言效果
						if (cha instanceof L1PcInstance) {
							final L1PcInstance pc = (L1PcInstance) cha;
							pc.sendPackets(new S_ServerMessage(288)); // 現在可以聊天了。
						}
						break;

					case STATUS_POISON: // 毒素效果
						cha.curePoison();
						break;

					case STATUS_SUNRISE:
						if (cha instanceof L1PcInstance) {
							final L1PcInstance pc = (L1PcInstance) cha;
							pc.sendPackets(new S_IconConfig(S_IconConfig.SKILL_ICON, 20, 0, true));
						}
						break;

					case STATUS_DRAGON_GEM_EXP:
						if (cha instanceof L1PcInstance) {
							final L1PcInstance pc = (L1PcInstance) cha;
							DragonGemBuff.end(pc);
						}
						break;

					case COOKING_1_0_N:
					case COOKING_1_0_S: // 漂浮眼球牛排
						if (cha instanceof L1PcInstance) {
							final L1PcInstance pc = (L1PcInstance) cha;
							pc.addWind(-10);
							pc.addWater(-10);
							pc.addFire(-10);
							pc.addEarth(-10);
							pc.sendPackets(new S_OwnCharAttrDef(pc));
							pc.sendPackets(new S_PacketBoxCooking(pc, 0, 0));
							pc.setCookingId(0);
						}
						break;

					case COOKING_1_1_N:
					case COOKING_1_1_S: // 熊肉牛排
						if (cha instanceof L1PcInstance) {
							final L1PcInstance pc = (L1PcInstance) cha;
							pc.addMaxHp(-30);
							pc.sendPackets(new S_HPUpdate(pc.getCurrentHp(), pc.getMaxHp()));
							if (pc.isInParty()) { // 隊伍中
								pc.getParty().updateMiniHP(pc);
							}
							pc.sendPackets(new S_PacketBoxCooking(pc, 1, 0));
							pc.setCookingId(0);
						}
						break;

					case COOKING_1_2_N:
					case COOKING_1_2_S: // 堅果麻糬
						if (cha instanceof L1PcInstance) {
							final L1PcInstance pc = (L1PcInstance) cha;
							pc.sendPackets(new S_PacketBoxCooking(pc, 2, 0));
							pc.setCookingId(0);
						}
						break;

					case COOKING_1_3_N:
					case COOKING_1_3_S: // 蟻腳乳酪燒
						if (cha instanceof L1PcInstance) {
							final L1PcInstance pc = (L1PcInstance) cha;
							pc.addAc(1);
							pc.sendPackets(new S_PacketBoxCooking(pc, 3, 0));
							pc.setCookingId(0);
						}
						break;

					case COOKING_1_4_N:
					case COOKING_1_4_S: // 水果沙拉
						if (cha instanceof L1PcInstance) {
							final L1PcInstance pc = (L1PcInstance) cha;
							pc.addMaxMp(-20);
							pc.sendPackets(new S_MPUpdate(pc.getCurrentMp(), pc.getMaxMp()));
							pc.sendPackets(new S_PacketBoxCooking(pc, 4, 0));
							pc.setCookingId(0);
						}
						break;

					case COOKING_1_5_N:
					case COOKING_1_5_S: // 水果糖醋勾芡
						if (cha instanceof L1PcInstance) {
							final L1PcInstance pc = (L1PcInstance) cha;
							pc.sendPackets(new S_PacketBoxCooking(pc, 5, 0));
							pc.setCookingId(0);
						}
						break;

					case COOKING_1_6_N:
					case COOKING_1_6_S: // 豬肉串燒
						if (cha instanceof L1PcInstance) {
							final L1PcInstance pc = (L1PcInstance) cha;
							pc.addMr(-5);
							pc.sendPackets(new S_SPMR(pc));
							pc.sendPackets(new S_PacketBoxCooking(pc, 6, 0));
							pc.setCookingId(0);
						}
						break;

					case COOKING_1_7_N:
					case COOKING_1_7_S: // 蘑菇湯
						if (cha instanceof L1PcInstance) {
							final L1PcInstance pc = (L1PcInstance) cha;
							pc.sendPackets(new S_PacketBoxCooking(pc, 7, 0));
							pc.setDessertId(0);
						}
						break;

					case COOKING_2_0_N:
					case COOKING_2_0_S: // 魚子醬抹醬
						if (cha instanceof L1PcInstance) {
							final L1PcInstance pc = (L1PcInstance) cha;
							pc.sendPackets(new S_PacketBoxCooking(pc, 8, 0));
							pc.setCookingId(0);
						}
						break;

					case COOKING_2_1_N:
					case COOKING_2_1_S: // 鱷魚牛排
						if (cha instanceof L1PcInstance) {
							final L1PcInstance pc = (L1PcInstance) cha;
							pc.addMaxHp(-30);
							pc.sendPackets(new S_HPUpdate(pc.getCurrentHp(), pc.getMaxHp()));
							if (pc.isInParty()) { // 隊伍中
								pc.getParty().updateMiniHP(pc);
							}
							pc.addMaxMp(-30);
							pc.sendPackets(new S_MPUpdate(pc.getCurrentMp(), pc.getMaxMp()));
							pc.sendPackets(new S_PacketBoxCooking(pc, 9, 0));
							pc.setCookingId(0);
						}
						break;

					case COOKING_2_2_N:
					case COOKING_2_2_S: // 龜龍點心
						if (cha instanceof L1PcInstance) {
							final L1PcInstance pc = (L1PcInstance) cha;
							pc.addAc(2);
							pc.sendPackets(new S_PacketBoxCooking(pc, 10, 0));
							pc.setCookingId(0);
						}
						break;

					case COOKING_2_3_N:
					case COOKING_2_3_S: // 奇異果鸚鵡燒
						if (cha instanceof L1PcInstance) {
							final L1PcInstance pc = (L1PcInstance) cha;
							pc.sendPackets(new S_PacketBoxCooking(pc, 11, 0));
							pc.setCookingId(0);
						}
						break;

					case COOKING_2_4_N:
					case COOKING_2_4_S: // 蠍子燒
						if (cha instanceof L1PcInstance) {
							final L1PcInstance pc = (L1PcInstance) cha;
							pc.sendPackets(new S_PacketBoxCooking(pc, 12, 0));
							pc.setCookingId(0);
						}
						break;

					case COOKING_2_5_N:
					case COOKING_2_5_S: // 邪惡蜥蜴燉肉
						if (cha instanceof L1PcInstance) {
							final L1PcInstance pc = (L1PcInstance) cha;
							pc.addMr(-10);
							pc.sendPackets(new S_SPMR(pc));
							pc.sendPackets(new S_PacketBoxCooking(pc, 13, 0));
							pc.setCookingId(0);
						}
						break;

					case COOKING_2_6_N:
					case COOKING_2_6_S: // 蜘蛛腳串燒
						if (cha instanceof L1PcInstance) {
							final L1PcInstance pc = (L1PcInstance) cha;
							pc.addSp(-1);
							pc.sendPackets(new S_SPMR(pc));
							pc.sendPackets(new S_PacketBoxCooking(pc, 14, 0));
							pc.setCookingId(0);
						}
						break;

					case COOKING_2_7_N:
					case COOKING_2_7_S: // 蟹肉湯
						if (cha instanceof L1PcInstance) {
							final L1PcInstance pc = (L1PcInstance) cha;
							pc.sendPackets(new S_PacketBoxCooking(pc, 15, 0));
							pc.setDessertId(0);
						}
						break;

					case COOKING_3_0_N:
					case COOKING_3_0_S: // 克拉肯鉗肉燒
						if (cha instanceof L1PcInstance) {
							final L1PcInstance pc = (L1PcInstance) cha;
							pc.sendPackets(new S_PacketBoxCooking(pc, 16, 0));
							pc.setCookingId(0);
						}
						break;

					case COOKING_3_1_N:
					case COOKING_3_1_S: // 獅鷲燒
						if (cha instanceof L1PcInstance) {
							final L1PcInstance pc = (L1PcInstance) cha;
							pc.addMaxHp(-50);
							pc.sendPackets(new S_HPUpdate(pc.getCurrentHp(), pc.getMaxHp()));
							if (pc.isInParty()) { // 隊伍中
								pc.getParty().updateMiniHP(pc);
							}
							pc.addMaxMp(-50);
							pc.sendPackets(new S_MPUpdate(pc.getCurrentMp(), pc.getMaxMp()));
							pc.sendPackets(new S_PacketBoxCooking(pc, 17, 0));
							pc.setCookingId(0);
						}
						break;

					case COOKING_3_2_N:
					case COOKING_3_2_S: // 雞蛇牛排
						if (cha instanceof L1PcInstance) {
							final L1PcInstance pc = (L1PcInstance) cha;
							pc.sendPackets(new S_PacketBoxCooking(pc, 18, 0));
							pc.setCookingId(0);
						}
						break;

					case COOKING_3_3_N:
					case COOKING_3_3_S: // 龜龍燒
						if (cha instanceof L1PcInstance) {
							final L1PcInstance pc = (L1PcInstance) cha;
							pc.addAc(3);
							pc.sendPackets(new S_PacketBoxCooking(pc, 19, 0));
							pc.setCookingId(0);
						}
						break;

					case COOKING_3_4_N:
					case COOKING_3_4_S: // 小龍翅
						if (cha instanceof L1PcInstance) {
							final L1PcInstance pc = (L1PcInstance) cha;
							pc.addMr(-15);
							pc.sendPackets(new S_SPMR(pc));
							pc.addWind(-10);
							pc.addWater(-10);
							pc.addFire(-10);
							pc.addEarth(-10);
							pc.sendPackets(new S_OwnCharAttrDef(pc));
							pc.sendPackets(new S_PacketBoxCooking(pc, 20, 0));
							pc.setCookingId(0);
						}
						break;

					case COOKING_3_5_N:
					case COOKING_3_5_S: // 飛龍燒
						if (cha instanceof L1PcInstance) {
							final L1PcInstance pc = (L1PcInstance) cha;
							pc.addSp(-2);
							pc.sendPackets(new S_SPMR(pc));
							pc.sendPackets(new S_PacketBoxCooking(pc, 21, 0));
							pc.setCookingId(0);
						}
						break;

					case COOKING_3_6_N:
					case COOKING_3_6_S: // 深海魚燉肉
						if (cha instanceof L1PcInstance) {
							final L1PcInstance pc = (L1PcInstance) cha;
							pc.addMaxHp(-30);
							pc.sendPackets(new S_HPUpdate(pc.getCurrentHp(), pc.getMaxHp()));
							if (pc.isInParty()) { // 隊伍中
								pc.getParty().updateMiniHP(pc);
							}
							pc.sendPackets(new S_PacketBoxCooking(pc, 22, 0));
							pc.setCookingId(0);
						}
						break;

					case COOKING_3_7_N:
					case COOKING_3_7_S: // 蛇雞蛋湯
						if (cha instanceof L1PcInstance) {
							final L1PcInstance pc = (L1PcInstance) cha;
							pc.sendPackets(new S_PacketBoxCooking(pc, 23, 0));
							pc.setDessertId(0);
						}
						break;

					case COOKING_4_0_N: // 強壯的牛排
						if (cha instanceof L1PcInstance) {
							final L1PcInstance pc = (L1PcInstance) cha;
							pc.addMr(-10);
							pc.addEarth(-10);
							pc.addWater(-10);
							pc.addFire(-10);
							pc.addWind(-10);
							pc.addHpr(-2);
							pc.addMpr(-2);
							pc.sendPackets(new S_SPMR(pc));
							pc.sendPackets(new S_OwnCharAttrDef(pc));
							pc.sendPackets(new S_PacketBoxCooking(pc, 157, 0));
							pc.setCookingId(0);
						}
						break;

					case COOKING_4_1_N: // 敏捷的煎鮭魚
						if (cha instanceof L1PcInstance) {
							final L1PcInstance pc = (L1PcInstance) cha;
							pc.addMr(-10);
							pc.addEarth(-10);
							pc.addWater(-10);
							pc.addFire(-10);
							pc.addWind(-10);
							pc.addHpr(-2);
							pc.addMpr(-2);
							pc.sendPackets(new S_SPMR(pc));
							pc.sendPackets(new S_OwnCharAttrDef(pc));
							pc.sendPackets(new S_PacketBoxCooking(pc, 158, 0));
							pc.setCookingId(0);
						}
						break;

					case COOKING_4_2_N: // 炭烤的火雞
						if (cha instanceof L1PcInstance) {
							final L1PcInstance pc = (L1PcInstance) cha;
							pc.addSp(-2);
							pc.addMr(-10);
							pc.addEarth(-10);
							pc.addWater(-10);
							pc.addFire(-10);
							pc.addWind(-10);
							pc.addHpr(-2);
							pc.addMpr(-3);
							pc.sendPackets(new S_SPMR(pc));
							pc.sendPackets(new S_OwnCharAttrDef(pc));
							pc.sendPackets(new S_PacketBoxCooking(pc, 159, 0));
							pc.setCookingId(0);
						}
						break;

					case COOKING_4_3_N: // 養生的雞湯
						if (cha instanceof L1PcInstance) {
							final L1PcInstance pc = (L1PcInstance) cha;
							pc.sendPackets(new S_PacketBoxCooking(pc, 160, 0));
							pc.setDessertId(0);
						}
						break;
				}
			}
			// cha.removeSkillEffect(skillId);

		} catch (final Exception e) {
			_log.error(e.getLocalizedMessage(), e);
		}

		if (cha instanceof L1PcInstance) {
			final L1PcInstance pc = (L1PcInstance) cha;
			sendStopMessage(pc, skillId);
			pc.sendPackets(new S_OwnCharStatus(pc));
		}
	}

	// 顯示訊息（技能結束時）
	private static void sendStopMessage(final L1PcInstance charaPc, final int skillid) {
		final L1Skills l1skills = SkillsTable.get().getTemplate(skillid);
		if ((l1skills == null) || (charaPc == null)) {
			return;
		}

		final int msgID = l1skills.getSysmsgIdStop();
		if (msgID > 0) {
			charaPc.sendPackets(new S_ServerMessage(msgID));
		}
	}
}
