package de.frank.jmh.model;

import de.frank.impl.jaxb.adapter.*;
import jakarta.xml.bind.annotation.*;
import jakarta.xml.bind.annotation.adapters.*;
import lombok.*;
import lombok.experimental.*;

import java.io.*;
import java.time.*;
import java.util.*;
import java.util.function.*;

import static de.frank.jmh.model.HierarchicalMockModel.DeepCopyUtil.*;

@Data
@Builder
@NoArgsConstructor //required by de-serializers
@AllArgsConstructor//required for builder to support default values
@FieldDefaults(level = AccessLevel.PRIVATE)//auto-add private in front of all fields
//javax.xml.bind annotations:
@XmlType(propOrder = {})//tell jaxb to generate xs:all instead of xs:sequence (we dont care about ordering)
@XmlAccessorType(XmlAccessType.FIELD) //bind by Fields and not by methods
@XmlRootElement(name = "HierarchicalMockModel")
public class HierarchicalMockModel implements Serializable {
    String name;

    @Singular
    @XmlElement(name = "ModelLevel1")//element name matches Type and not the field name
    @XmlElementWrapper(name = "subTypes")
    List<ModelLevel1> subTypes = new ArrayList<>();

    /**
     * Copy constructor
     *
     * @param src the source
     */
    public HierarchicalMockModel(HierarchicalMockModel src) {
        this(src.name, deepCopyList(src.subTypes, ModelLevel1::new));
    }

    public static HierarchicalMockModel newInstance(int level1Nodes, int level2Nodes, int level3Nodes) {
        return newInstance("", level1Nodes, level2Nodes, level3Nodes);
    }

    public static HierarchicalMockModel newInstance(String prefix, int level1Nodes, int level2Nodes, int level3Nodes) {
        var startDate = OffsetDateTime.of(2021, 1, 1, 1, 1, 1, 1, ZoneOffset.UTC);

        var root = new HierarchicalMockModel();
        root.setName(prefix + "root");

        for (int level1Idx = 0; level1Idx < level1Nodes; level1Idx++) {
            var level1Instance = new ModelLevel1();
            level1Instance.setName(prefix + "modelLevel1_" + level1Idx);
            level1Instance.setAInt(level1Idx);
            level1Instance.setADouble(level1Idx);
            level1Instance.setADate(startDate.plusHours(level1Idx));

            for (int level2Idx = 0; level2Idx < level2Nodes; level2Idx++) {
                var level2Instance = new ModelLevel2();
                level2Instance.setName(prefix + "modelLevel2_" + level1Idx + "_" + level2Idx);
                level2Instance.setAInt(level2Idx);
                level2Instance.setADouble(level2Idx);
                level2Instance.setADate(startDate.plusMinutes(level2Idx));

                for (int level3Idx = 0; level3Idx < level3Nodes; level3Idx++) {
                    var level3Instance = new ModelLevel3();
                    level2Instance.setName(prefix + "modelLevel3_" + level1Idx + "_" + level2Idx + "_" +
                                           level3Idx);

                    level3Instance.setAInt(level3Idx);
                    level3Instance.setADouble(level3Idx);
                    level3Instance.setADate(startDate.plusSeconds(level3Idx));

                    level2Instance.getSubTypes().add(level3Instance);
                }
                level1Instance.getSubTypes().add(level2Instance);
            }
            root.getSubTypes().add(level1Instance);
        }
        return root;
    }

    @Data
    @Builder
    @NoArgsConstructor //required by de-serializers
    @AllArgsConstructor//required for builder to support default values
    @FieldDefaults(level = AccessLevel.PRIVATE)//auto-add private in front of all fields
//javax.xml.bind annotations:
    @XmlType(propOrder = {})//tell jaxb to generate xs:all instead of xs:sequence (we dont care about ordering)
    @XmlAccessorType(XmlAccessType.FIELD) //bind by Fields and not by methods
    public static class ModelLevel1 implements Serializable {

        String name;

        int aInt;

        double aDouble;
        @XmlJavaTypeAdapter(OffsetDateTimeXmlAdapter.class)
        OffsetDateTime aDate;


        @Singular
        @XmlElement(name = "ModelLevel2")//element name matches Type and not the field name
        @XmlElementWrapper(name = "subTypes")
        List<ModelLevel2> subTypes = new ArrayList<>();

        /**
         * Copy constructor
         *
         * @param src the source
         */
        public ModelLevel1(ModelLevel1 src) {
            this(src.name, src.aInt, src.aDouble, src.aDate, deepCopyList(src.subTypes, ModelLevel2::new));
        }
    }

    @Data
    @Builder
    @NoArgsConstructor //required by de-serializers
    @AllArgsConstructor//required for builder to support default values
    @FieldDefaults(level = AccessLevel.PRIVATE)//auto-add private in front of all fields
//javax.xml.bind annotations:
    @XmlType(propOrder = {})//tell jaxb to generate xs:all instead of xs:sequence (we dont care about ordering)
    @XmlAccessorType(XmlAccessType.FIELD) //bind by Fields and not by methods
    public static class ModelLevel2 implements Serializable {

        String name;

        int aInt;

        double aDouble;

        @XmlJavaTypeAdapter(OffsetDateTimeXmlAdapter.class)
        OffsetDateTime aDate;

        @Singular
        @XmlElement(name = "ModelLevel3")//element name matches Type and not the field name
        @XmlElementWrapper(name = "subTypes")
        List<ModelLevel3> subTypes = new ArrayList<>();

        /**
         * Copy constructor
         *
         * @param src the source
         */
        public ModelLevel2(ModelLevel2 src) {
            this(src.name, src.aInt, src.aDouble, src.aDate, deepCopyList(src.subTypes, ModelLevel3::new));
        }

    }

    @Data
    @Builder
    @NoArgsConstructor //required by de-serializers
    @AllArgsConstructor//required for builder to support default values
    @FieldDefaults(level = AccessLevel.PRIVATE)//auto-add private in front of all fields
//javax.xml.bind annotations:
    @XmlType(propOrder = {})//tell jaxb to generate xs:all instead of xs:sequence (we dont care about ordering)
    @XmlAccessorType(XmlAccessType.FIELD) //bind by Fields and not by methods
    public static class ModelLevel3 implements Serializable {
        String name;

        int aInt;

        double aDouble;

        @XmlJavaTypeAdapter(OffsetDateTimeXmlAdapter.class)
        OffsetDateTime aDate;

        /**
         * Copy constructor
         *
         * @param src the source
         */
        public ModelLevel3(ModelLevel3 src) {
            this(src.name, src.aInt, src.aDouble, src.aDate);
        }
    }

    public static class DeepCopyUtil {

        public static <T> List<T> deepCopyList(List<T> src, UnaryOperator<T> copyFunction) {
            if (src == null) {
                return null;
            }
            if (src.isEmpty()) {
                return new ArrayList<>();
            }
            ArrayList<T> result = new ArrayList<>(src.size());
            for (T t : src) {
                result.add(copyFunction.apply(t));
            }
            return result;
        }
    }
}

