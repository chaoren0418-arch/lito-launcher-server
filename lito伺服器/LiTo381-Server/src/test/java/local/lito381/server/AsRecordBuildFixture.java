package local.lito381.server;

/** 只進測試 JAR；供 Maven 生命週期與封裝驗證。 */
@AsRecord(components = {"id"})
public final class AsRecordBuildFixture {
    private final int id;
    int extra;
    static int ignoredStatic;

    public AsRecordBuildFixture(int id) { this.id = id; }
    public int id() { return id; }

    static final class Access {
        static int update(AsRecordBuildFixture fixture, int value) {
            fixture.extra = value;
            return fixture.extra;
        }
    }
}
