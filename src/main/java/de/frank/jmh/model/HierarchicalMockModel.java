package de.frank.jmh.model;

import lombok.*;
import lombok.experimental.*;

import javax.xml.bind.annotation.*;
import java.time.*;
import java.util.*;

@Data
@Builder
@NoArgsConstructor //required by de-serializers
@AllArgsConstructor//required for builder to support default values
@FieldDefaults(level = AccessLevel.PRIVATE)//auto-add private in front of all fields
//javax.xml.bind annotations:
@XmlType(propOrder = {})//tell jaxb schemagen to generate xs:all instead of xs:sequence (we dont care about ordering)
@XmlAccessorType(XmlAccessType.FIELD) //bind by Fields and not by methods
@XmlRootElement(name = "HierarchicalMockModel")
public class HierarchicalMockModel {
    String name;
    @Singular
    List<ModelLevel1> subTypes;

    public static HierarchicalMockModel newInstance(int level1Nodes, int level2Nodes, int level3Nodes) {
        var startDate = OffsetDateTime.of(2021, 1, 1, 1, 1, 1, 1, ZoneOffset.UTC);
        var root = HierarchicalMockModel.builder().name("root");
        for (int level1Idx = 0; level1Idx < level1Nodes; level1Idx++) {
            var level1Instance = ModelLevel1.builder()
                                            .name("modelLevel1_" + level1Idx)
                                            .aInt(level1Idx)
                                            .aDouble(level1Idx)
                                            .aDate(startDate.plusHours(level1Idx));
            for (int level2Idx = 0; level2Idx < level2Nodes; level2Idx++) {
                var level2Instance = ModelLevel2.builder()
                                                .name("modelLevel2_" + level1Idx + "_" + level2Idx)
                                                .aInt(level2Idx)
                                                .aDouble(level2Idx)
                                                .aDate(startDate.plusMinutes(level2Idx));

                for (int level3Idx = 0; level3Idx < level3Nodes; level3Idx++) {
                    var level3Instance = ModelLevel3.builder()
                                                    .name("modelLevel3_" + level1Idx + "_" + level2Idx + "_" +
                                                          level3Idx)
                                                    .aInt(level3Idx)
                                                    .aDouble(level3Idx)
                                                    .aDate(startDate.plusSeconds(level3Idx));

                    level2Instance.subType(level3Instance.build());
                }
                level1Instance.subType(level2Instance.build());
            }
            root.subType(level1Instance.build());
        }
        return root.build();
    }

    @Data
    @Builder
    @NoArgsConstructor //required by de-serializers
    @AllArgsConstructor//required for builder to support default values
    @FieldDefaults(level = AccessLevel.PRIVATE)//auto-add private in front of all fields
//javax.xml.bind annotations:
    @XmlType(propOrder = {})
//tell jaxb schemagen to generate xs:all instead of xs:sequence (we dont care about ordering)
    @XmlAccessorType(XmlAccessType.FIELD) //bind by Fields and not by methods
    public static class ModelLevel1 {
        String name;
        int aInt;
        double aDouble;
        OffsetDateTime aDate;
        @Singular
        List<ModelLevel2> subTypes;

    }

    @Data
    @Builder
    @NoArgsConstructor //required by de-serializers
    @AllArgsConstructor//required for builder to support default values
    @FieldDefaults(level = AccessLevel.PRIVATE)//auto-add private in front of all fields
//javax.xml.bind annotations:
    @XmlType(propOrder = {})
//tell jaxb schemagen to generate xs:all instead of xs:sequence (we dont care about ordering)
    @XmlAccessorType(XmlAccessType.FIELD) //bind by Fields and not by methods
    public static class ModelLevel2 {
        String name;
        int aInt;
        double aDouble;
        OffsetDateTime aDate;
        @Singular
        List<ModelLevel3> subTypes;
    }

    @Data
    @Builder
    @NoArgsConstructor //required by de-serializers
    @AllArgsConstructor//required for builder to support default values
    @FieldDefaults(level = AccessLevel.PRIVATE)//auto-add private in front of all fields
//javax.xml.bind annotations:
    @XmlType(propOrder = {})
//tell jaxb schemagen to generate xs:all instead of xs:sequence (we dont care about ordering)
    @XmlAccessorType(XmlAccessType.FIELD) //bind by Fields and not by methods
    public static class ModelLevel3 {
        String name;
        int aInt;
        double aDouble;
        OffsetDateTime aDate;
    }
}

