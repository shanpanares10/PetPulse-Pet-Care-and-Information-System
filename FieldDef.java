package petpulse.ui;

import java.time.LocalDate;

public final class FieldDef {

    public enum Type { TEXT, NUMBER, DATE, COMBO, PET }

    public final String label;
    public final String column;
    public final Type type;
    public final boolean required;
    public final String[] options;
    public final boolean defaultToday;

    private FieldDef(String label, String column, Type type, boolean required,
            String[] options, boolean defaultToday) {
        this.label = label;
        this.column = column;
        this.type = type;
        this.required = required;
        this.options = options;
        this.defaultToday = defaultToday;
    }

    public static FieldDef text(String label, String column, boolean required) {
        return new FieldDef(label, column, Type.TEXT, required, null, false);
    }

    public static FieldDef number(String label, String column, boolean required) {
        return new FieldDef(label, column, Type.NUMBER, required, null, false);
    }

    /** Date typed as yyyy-MM-dd. */
    public static FieldDef date(String label, String column, boolean required, boolean defaultToday) {
        return new FieldDef(label, column, Type.DATE, required, null, defaultToday);
    }

    public static FieldDef combo(String label, String column, String... options) {
        return new FieldDef(label, column, Type.COMBO, true, options, false);
    }

    /** Drop-down of pets (stores pet_id). */
    public static FieldDef pet() {
        return new FieldDef("Pet", "pet_id", Type.PET, true, null, false);
    }

    public String todayText() {
        return LocalDate.now().toString();
    }
}
