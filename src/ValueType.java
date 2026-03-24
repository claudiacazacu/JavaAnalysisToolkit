public enum ValueType {
    INT("int"),
    BOOL("bool"),
    STRING("string");

    private final String keyword;

    ValueType(String keyword) {
        this.keyword = keyword;
    }

    public String keyword() {
        return keyword;
    }

    public static ValueType fromKeyword(String keyword) {
        for (ValueType type : values()) {
            if (type.keyword.equals(keyword)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown type keyword: " + keyword);
    }
}
