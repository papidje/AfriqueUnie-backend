package friasoft.gn.schoolapp.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

//@AllArgsConstructor
@Getter
public enum RoleEnum {
    SUPER_ADMIN(1),
    ADMINISTRATOR(2),
    ACCOUNTER(3),
    TEACHER(4);
    
    private final int id;

    RoleEnum (int id) {
        this.id = id;
    }

    public static RoleEnum getById(int id) {
        for(RoleEnum e : values()) {
            if(e.id == id) return e;
        }
        throw new IllegalArgumentException();
    }
}
