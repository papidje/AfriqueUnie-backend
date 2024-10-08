package friasoft.gn.schoolapp.enums;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum RoleEnum {
    ADMINISTRATOR(1, 'X'),
    DIRECTOR(2, 'D'),
    ACCOUNTER(3, 'A'),
    INSTRUCTOR(4, 'I');
    
    private final int id;
    private final char code;

    public static RoleEnum getById(int id) {
        for(RoleEnum e : values()) {
            if(e.id == id) return e;
        }
        throw new IllegalArgumentException();
    }
    
    public static RoleEnum getByCode(char code) {
        for(RoleEnum e : values()) {
            if(e.code == code) return e;
        }
        throw new IllegalArgumentException();
    }
}
