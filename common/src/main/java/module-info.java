module org.example.ishtirak.common {
    requires static jakarta.validation;

    exports org.example.ishtirak.common.dto;
    exports org.example.ishtirak.common.model;

    // Jackson reads/creates the records by reflection
    opens org.example.ishtirak.common.dto;
}
