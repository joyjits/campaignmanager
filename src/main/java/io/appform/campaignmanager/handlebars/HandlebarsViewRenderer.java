package io.appform.campaignmanager.handlebars;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.cache.GuavaTemplateCache;
import com.github.jknack.handlebars.helper.ConditionalHelpers;
import com.github.jknack.handlebars.io.TemplateSource;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.dropwizard.views.View;
import io.dropwizard.views.ViewRenderer;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * A {@link ViewRenderer} which renders Handlebars ({@code .hbs}) templates.
 */
public class HandlebarsViewRenderer implements ViewRenderer {
    /**
     * For use by Handlebars.java internally.
     */
    private static final Cache<TemplateSource, Template> templateCache = CacheBuilder
            .newBuilder()
            .build();

    /**
     * Handlebars.java does not cache reads of Template content from resources.
     */
    @VisibleForTesting
    static final LoadingCache<String, Template> compilationCache = CacheBuilder
            .newBuilder()
            .build(new CacheLoader<String, Template>() {
                @Override
                public Template load(String srcUrl) throws Exception {
                    return HANDLEBARS.compile(srcUrl.replaceAll(".hbs$", ""));
                }
            });

    /**
     * Exposed for use in {@link HandlebarsHelperBundle} for miscellaneous configuration.
     */
    static final Handlebars HANDLEBARS = new Handlebars()
            .with(new GuavaTemplateCache(templateCache))
            .registerHelper("eq", ConditionalHelpers.eq)
            .registerHelper("neq", ConditionalHelpers.neq)
            .registerHelper("not", ConditionalHelpers.not)
            .registerHelper("or", ConditionalHelpers.or)
            .registerHelper("and", ConditionalHelpers.and)
            .registerHelper("gt", ConditionalHelpers.gt)
            .registerHelper("gte", ConditionalHelpers.gte)
            .registerHelper("lt", ConditionalHelpers.lt)
            .registerHelper("lte", ConditionalHelpers.lte)
            .registerHelpers(new CustomHelpers())
            ;

    public HandlebarsViewRenderer() {
        //do nothing
    }

    @Override
    public boolean isRenderable(View view) {
        return view.getTemplateName().endsWith(".hbs");
//                || view.getTemplateName().endsWith(".mustache"); // we can replace dropwizard-views-mustache with this.
    }

    @Override
    public void render(View view, Locale locale, OutputStream output) throws IOException {
        try (Writer writer = new OutputStreamWriter(output, view.getCharset().orElse(StandardCharsets.UTF_8))) {
            compilationCache.get(view.getTemplateName()).apply(view, writer);
        } catch (FileNotFoundException | ExecutionException e) {
            throw new FileNotFoundException("Template " + view.getTemplateName() + " not found.");
        }
    }

    @Override
    public void configure(Map<String, String> options) {
        //do nothing
    }

    @Override
    public String getConfigurationKey() {
        return "handlebars";
    }
}
