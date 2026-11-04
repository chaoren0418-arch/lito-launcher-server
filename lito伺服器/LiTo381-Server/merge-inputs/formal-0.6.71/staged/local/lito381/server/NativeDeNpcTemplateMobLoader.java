// DECOMPILED from base-0.6.71.jar, TODO review
package local.lito381.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import local.lito381.server.NativeDeNpcMobSkill;
import local.lito381.server.NativeDeNpcTemplate;
import local.lito381.server.NativeDeNpcTemplateLoadScope;

final class NativeDeNpcTemplateMobLoader {
    NativeDeNpcTemplateMobLoader() {
    }

    static void load() {
        new NativeDeNpcTemplateMobLoader().sourceLoadMobSkillData();
    }

    private void sourceLoadMobSkillData() {
        NativeDeNpcTemplateLoadScope.Timer timer = new NativeDeNpcTemplateLoadScope.Timer();
        try (Connection connection = NativeDeNpcTemplateLoadScope.Database.get().getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT mobid,count(*) as cnt FROM mobskill group by mobid");
             PreparedStatement preparedStatement2 = connection.prepareStatement("SELECT * FROM mobskill where mobid = ? order by mobid,actNo");
             ResultSet resultSet = preparedStatement.executeQuery();){
            int n = 0;
            int n2 = 0;
            int n3 = 0;
            while (resultSet.next()) {
                n2 = resultSet.getInt("mobid");
                n = resultSet.getInt("cnt");
                try {
                    preparedStatement2.setInt(1, n2);
                    NativeDeNpcMobSkill nativeDeNpcMobSkill = new NativeDeNpcMobSkill(n);
                    nativeDeNpcMobSkill.set_mobid(n2);
                    try (ResultSet resultSet2 = preparedStatement2.executeQuery();){
                        while (resultSet2.next()) {
                            n3 = resultSet2.getInt("actNo");
                            String string = resultSet2.getString("mobname");
                            int n4 = resultSet2.getInt("type");
                            int n5 = resultSet2.getInt("SkillId");
                            nativeDeNpcMobSkill.setMobName(NativeDeNpcTemplateMobLoader.isBlank(string) ? NativeDeNpcTemplateMobLoader.buildMobSkillName(n2, n5, n4) : string);
                            nativeDeNpcMobSkill.setType(n3, resultSet2.getInt("type"));
                            nativeDeNpcMobSkill.setTriggerRandom(n3, resultSet2.getInt("TriRnd"));
                            nativeDeNpcMobSkill.setTriggerHp(n3, resultSet2.getInt("TriHp"));
                            nativeDeNpcMobSkill.setTriggerCompanionHp(n3, resultSet2.getInt("TriCompanionHp"));
                            nativeDeNpcMobSkill.setTriggerRange(n3, resultSet2.getInt("TriRange"));
                            nativeDeNpcMobSkill.setTriggerCount(n3, resultSet2.getInt("TriCount"));
                            nativeDeNpcMobSkill.setChangeTarget(n3, resultSet2.getInt("ChangeTarget"));
                            nativeDeNpcMobSkill.setRange(n3, resultSet2.getInt("Range"));
                            nativeDeNpcMobSkill.setAreaWidth(n3, resultSet2.getInt("AreaWidth"));
                            nativeDeNpcMobSkill.setAreaHeight(n3, resultSet2.getInt("AreaHeight"));
                            nativeDeNpcMobSkill.setLeverage(n3, resultSet2.getInt("Leverage"));
                            nativeDeNpcMobSkill.setSkillId(n3, resultSet2.getInt("SkillId"));
                            nativeDeNpcMobSkill.setGfxid(n3, resultSet2.getInt("Gfxid"));
                            nativeDeNpcMobSkill.setActid(n3, resultSet2.getInt("Actid"));
                            nativeDeNpcMobSkill.setSummon(n3, resultSet2.getInt("SummonId"));
                            nativeDeNpcMobSkill.setSummonMin(n3, resultSet2.getInt("SummonMin"));
                            nativeDeNpcMobSkill.setSummonMax(n3, resultSet2.getInt("SummonMax"));
                            nativeDeNpcMobSkill.setPolyId(n3, resultSet2.getInt("PolyId"));
                            nativeDeNpcMobSkill.setReuseDelay(n3, resultSet2.getInt("reuseDelay"));
                        }
                    }
                    NativeDeNpcTemplateLoadScope.mobSkills().put(n2, nativeDeNpcMobSkill);
                }
                catch (SQLException sQLException) {
                    NativeDeNpcTemplateLoadScope.log().error(sQLException.getLocalizedMessage(), sQLException);
                }
            }
            NativeDeNpcTemplateLoadScope.log().info("\u8f09\u5165MOB\u6280\u80fd\u8cc7\u6599\u6578\u91cf: " + NativeDeNpcTemplateLoadScope.mobSkills().size() + "(" + timer.get() + "ms)");
        }
        catch (SQLException sQLException) {
            NativeDeNpcTemplateLoadScope.log().error(sQLException.getLocalizedMessage(), sQLException);
        }
        NativeDeNpcTemplateLoadScope.suppressedBlankNames(this);
    }

    private static String buildMobSkillName(int n, int n2, int n3) {
        Object object;
        NativeDeNpcTemplate nativeDeNpcTemplate = NativeDeNpcTemplateLoadScope.Npcs.get().getTemplate(n);
        if (nativeDeNpcTemplate == null || NativeDeNpcTemplateMobLoader.isBlank(nativeDeNpcTemplate.get_name())) {
            return null;
        }
        String string = null;
        if (n2 > 0 && (object = NativeDeNpcTemplateLoadScope.Skills.get().getTemplate(n2)) != null) {
            string = ((NativeDeNpcTemplateLoadScope.Skill)object).getName();
        }
        if (NativeDeNpcTemplateMobLoader.isBlank(string)) {
            string = NativeDeNpcTemplateMobLoader.fallbackSkillName(n3, n2);
        }
        if (NativeDeNpcTemplateMobLoader.isBlank(string)) {
            return null;
        }
        object = nativeDeNpcTemplate.get_name() + ">" + string;
        return ((String)object).length() <= 45 ? (String)object : ((String)object).substring(0, 45);
    }

    private static String fallbackSkillName(int n, int n2) {
        switch (n) {
            case 1: {
                return "\u7269\u7406\u653b\u64ca";
            }
            case 2: {
                return n2 > 0 ? "\u6280\u80fd" + n2 : "\u9b54\u6cd5\u653b\u64ca";
            }
            case 3: {
                return "\u53ec\u559a";
            }
            case 4: {
                return "\u8b8a\u8eab";
            }
            case 5: {
                return "\u885d\u6688";
            }
            case 6: {
                return "\u9b54\u6cd5\u76f8\u6d88";
            }
            case 7: {
                return "\u6b66\u5668\u7834\u58de";
            }
        }
        return null;
    }

    private static boolean isBlank(String string) {
        return string == null || string.trim().isEmpty();
    }
}
