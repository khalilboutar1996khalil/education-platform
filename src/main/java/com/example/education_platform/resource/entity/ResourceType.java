package com.example.education_platform.resource.entity;

public enum ResourceType {
    PDF,
    VIDEO,
    ZIP,
    /** An address rather than an upload: the library stores the link, not the bytes. */
    LINK
}
