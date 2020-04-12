/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface ClassMapper {

    @Mapping(source = "propB1", target = "propA<caret>")
    ClassA toB(ClassB a);
}

public class ClassA {
    public String getPropA1() {
        return propA1;
    }

    public void setPropA1(String propA1) {
        this.propA1 = propA1;
    }

    public String getPropA2() {
        return propA2;
    }

    public void setPropA2(String propA2) {
        this.propA2 = propA2;
    }

    private String propA1;
    private String propA2;
}

public class ClassB {
    public String getPropB1() {
        return propB1;
    }

    public void setPropB1(String propB1) {
        this.propB1 = propB1;
    }

    public String getPropB2() {
        return propB2;
    }

    public void setPropB2(String propB2) {
        this.propB2 = propB2;
    }

    private String propB1;

    private String propB2;

}
