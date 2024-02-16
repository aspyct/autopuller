import io.javalin.Javalin;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AutoPuller {
    private static final Logger LOG = LoggerFactory.getLogger(AutoPuller.class);

    public static void main(String[] args) {
        var rootDirectory = new File(System.getenv("AUTOPULLER_ROOT"));

        var app = Javalin.create(/*config*/)
                .get("/", ctx -> ctx.result("Hello World"))
                .get("/health", ctx -> ctx.result("ok"))
                .post("/update/{component}", ctx -> {
                    var component = ctx.pathParam("component");

                    var validDirectories = listSubdirectories(rootDirectory);
                    if (validDirectories.contains(component)) {
                        boolean result = gitPull(rootDirectory, component);

                        if (result) {
                            ctx.result("pulled!");
                        }
                        else {
                            ctx.status(500);
                            ctx.result("failed");
                        }
                    }
                })
                .start(7070);
    }

    private static boolean gitPull(File rootDirectory, String component) throws IOException {
        var componentDirectory = new File(rootDirectory, component);
        var gitPull = Runtime.getRuntime().exec(new String[] {
            "/usr/bin/git",
            "pull"
        }, new String[] {}, componentDirectory);

        int status = 0;
        try {
            status = gitPull.waitFor();
        } catch (InterruptedException e) {
            status = -1;
        }
        LOG.info("Git pull status: {}", status);

        try (var reader = gitPull.errorReader()) {
            reader.lines().forEach(LOG::info);
        }
        try (var reader = gitPull.inputReader()) {
            reader.lines().forEach(LOG::info);
        }

        return status == 0;
    }

    private static Set<String> listSubdirectories(@NotNull File parent) {
        return Stream.of(Objects.requireNonNull(parent.listFiles()))
                .filter(File::isDirectory)
                .map(File::getName)
                .collect(Collectors.toSet());
    }
}