public class RuntimeValue {
    private final ValueType type;
    private final Object value;

    public RuntimeValue(ValueType type, Object value) {
        this.type = type;
        this.value = value;
    }

    public ValueType type() {
        return type;
    }

    public Object value() {
        return value;
    }

    public int asInt() {
        return (Integer) value;
    }

    public boolean asBoolean() {
        return (Boolean) value;
    }

    public String asString() {
        return (String) value;
    }

    public String displayValue() {
        return String.valueOf(value);
    }

    @Override
    public String toString() {
        return displayValue();
    }
}
