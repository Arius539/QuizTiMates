package org.hsw;

import javafx.application.Application;
import javafx.stage.Stage;
import org.hsw.display.navigation.ViewNavigator;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public class JavafxApp extends Application {

    private ConfigurableApplicationContext context;

    public JavafxApp() {
        this.context = null;
    }

    @Override
    public void init() {
        String[] args = getParameters().getRaw().toArray(new String[0]);
        context = new SpringApplicationBuilder(App.class).run(args);
    }

    @Override
    public void start(Stage stage) {
        ViewNavigator viewNavigator = context.getBean(ViewNavigator.class);
        viewNavigator.initialize(stage);
        viewNavigator.loadPlayerSetup();
    }

    @Override
    public void stop() {
        context.close();
    }
}
