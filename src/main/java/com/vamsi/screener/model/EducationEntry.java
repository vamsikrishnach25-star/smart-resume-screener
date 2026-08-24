package com.vamsi.screener.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** A single education row extracted from a resume. */
@Embeddable
public class EducationEntry {

    @Column(name = "institution", length = 300)
    private String institution;

    @Column(name = "degree", length = 200)
    private String degree;

    @Column(name = "field_of_study", length = 200)
    private String fieldOfStudy;

    /**
     * Free-form year or range, e.g. {@code "2019 - 2023"}.
     *
     * <p>The column is deliberately not called {@code year}: that is a reserved word in H2 and
     * several other databases, and using it makes the generated DDL fail.
     */
    @Column(name = "completion_year", length = 60)
    private String year;

    protected EducationEntry() {
        // required by JPA
    }

    public EducationEntry(String institution, String degree, String fieldOfStudy, String year) {
        this.institution = institution;
        this.degree = degree;
        this.fieldOfStudy = fieldOfStudy;
        this.year = year;
    }

    public String getInstitution() {
        return institution;
    }

    public String getDegree() {
        return degree;
    }

    public String getFieldOfStudy() {
        return fieldOfStudy;
    }

    public String getYear() {
        return year;
    }
}
