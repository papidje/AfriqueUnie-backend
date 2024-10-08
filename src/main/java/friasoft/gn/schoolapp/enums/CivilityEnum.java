package friasoft.gn.schoolapp.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CivilityEnum {
    MASCULIN(1, 'M'),
    FEMININ(2, 'F');

    private final int id;
    private final char code;

    public static CivilityEnum getById(int id) {
        for(CivilityEnum e : values()) {
            if(e.id == id) return e;
        }
        throw new IllegalArgumentException();
    }
    
    public static CivilityEnum getByCode(char code) {
        for(CivilityEnum e : values()) {
            if(e.code == code) return e;
        }
        throw new IllegalArgumentException();
    }
}
