package com.vamsi.screener.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** A single work-history row extracted from a resume. */
@Embeddable
public class ExperienceEntry {

    @Column(name = "company", length = 300)
    private String company;

    @Column(name = "title", length = 300)
    private String title;

    /** Free-form period, e.g. {@code "Jun 2022 - Present"}. */
    @Column(name = "period", length = 120)
    private String period;

    @Column(name = "description", length = 2000)
    private String description;

    protected ExperienceEntry() {
        // required by JPA
    }

    public ExperienceEntry(String company, String title, String period, String description) {
        this.company = company;
        this.title = title;
        this.period = period;
        this.description = description;
    }

    public String getCompany() {
        return company;
    }

    public String getTitle() {
        return title;
    }

    public String getPeriod() {
        return period;
    }

    public String getDescription() {
        return description;
    }
}
