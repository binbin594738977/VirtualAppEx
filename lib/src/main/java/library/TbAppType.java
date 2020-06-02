package library;

public enum TbAppType {
    none(""),
    taobao("com.taobao.taobao"),
    alibaba("com.alibaba.wireless"),
    tblm("com.alimama.moon"),
    ;

    public final String packageName;

    TbAppType(String packageName) {
        this.packageName = packageName;
    }

    public static TbAppType getType(String packageName) {
        for (TbAppType type : TbAppType.values()) {
            if (type.packageName.equals(packageName)) {
                return type;
            }
        }

        return none;
    }
}
