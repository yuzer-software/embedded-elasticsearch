package pl.allegro.tech.embeddedelasticsearch;

import java.util.function.Consumer;

/**
 * select java home to use when running elasticsearch
 */
public abstract class JavaHomeOption {

    public abstract boolean shouldBeSet();

    public abstract String getValue();

    public void ifNeedBeSet(Consumer<String> consumer) {
        if(shouldBeSet()) {
            consumer.accept(getValue());
        }
    }

    /** Lets elasticsearch startup script determine the JRE. */
    public static JavaHomeOption useSystem() {
        return new JavaHomeOption.UseSystem();
    }

    /** Use the JRE referenced by JAVA_HOME. */
    public static JavaHomeOption useJavaHome() {
        return new JavaHomeOption.UseJavaHome();
    }

    /** Use the JRE that is running the current process. */
    public static JavaHomeOption inheritTestSuite() {
        return new JavaHomeOption.Inherit();
    }

    /** Use the JRE referenced by the given path */
    public static JavaHomeOption path(String path) {
        return new JavaHomeOption.Path(path);
    }

    /** Lets elasticsearch startup script determine the JRE. */
    public static class UseSystem extends JavaHomeOption {

        public boolean shouldBeSet() {
            return false;
        }

        public String getValue() {
            return null;
        }
    }

    /** Use the JRE referenced by JAVA_HOME. */
    public static class UseJavaHome extends JavaHomeOption {

        public boolean shouldBeSet() {
            return true;
        }

        public String getValue() {
            return System.getenv("JAVA_HOME");
        }
    }

    /** Use the JRE that is running the current process. */
    public static class Inherit extends JavaHomeOption {

        @Override
        public boolean shouldBeSet() {
            return true;
        }

        @Override
        public String getValue() {
            return System.getProperty("java.home");
        }
    }

    /**
     * Use the JRE referenced by the given path, e.g.
     * new JavaHomeOption.Path("/usr/lib/jvm/java-8-openjdk-amd64/")
     */
    public static class Path extends JavaHomeOption {

        String value;

        public Path(String value) {
            this.value = value;
        }

        @Override
        public boolean shouldBeSet() {
            return true;
        }

        @Override
        public String getValue() {
            return value;
        }
    }

}
