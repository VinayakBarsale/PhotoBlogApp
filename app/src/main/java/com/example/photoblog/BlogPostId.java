package com.example.photoblog;

public class BlogPostId {

    public String BlogPostId;

    public <T extends BlogPostId> T withId(final String id)
    {
        this.BlogPostId=id;
        return (T) this;
    }
}
