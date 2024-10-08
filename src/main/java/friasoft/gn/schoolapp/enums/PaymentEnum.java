package friasoft.gn.schoolapp.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PaymentEnum {
    REGISTRATION(1, 'R'),
    SCHOOLING(2, 'S');

    private final int id;
    private final char code;

    public static PaymentEnum getById(int id) {
        for(PaymentEnum e : values()) {
            if(e.id == id) return e;
        }
        throw new IllegalArgumentException();
    }
    
    public static PaymentEnum getByCode(char code) {
        for(PaymentEnum e : values()) {
            if(e.code == code) return e;
        }
        throw new IllegalArgumentException();
    }
}
