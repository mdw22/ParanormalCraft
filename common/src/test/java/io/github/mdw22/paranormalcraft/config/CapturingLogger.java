package io.github.mdw22.paranormalcraft.config;

import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.slf4j.helpers.LegacyAbstractLogger;
import org.slf4j.helpers.MessageFormatter;

import java.util.ArrayList;
import java.util.List;

/**
 * Records log calls so tests can count warnings and errors.
 */
final class CapturingLogger extends LegacyAbstractLogger {

    record Entry(Level level, String message) {
    }

    final List<Entry> entries = new ArrayList<>();

    CapturingLogger() {

        this.name = "test";
    }

    long count(Level level) {

        return this.entries.stream().filter(entry -> entry.level() == level).count();
    }

    @Override
    protected String getFullyQualifiedCallerName() {

        return null;
    }

    @Override
    protected void handleNormalizedLoggingCall(Level level, Marker marker, String messagePattern, Object[] arguments, Throwable throwable) {

        this.entries.add(new Entry(level, MessageFormatter.basicArrayFormat(messagePattern, arguments)));
    }

    @Override
    public boolean isTraceEnabled() {

        return true;
    }

    @Override
    public boolean isDebugEnabled() {

        return true;
    }

    @Override
    public boolean isInfoEnabled() {

        return true;
    }

    @Override
    public boolean isWarnEnabled() {

        return true;
    }

    @Override
    public boolean isErrorEnabled() {

        return true;
    }
}
