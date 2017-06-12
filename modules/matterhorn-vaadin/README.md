This bundle is a fragment bundle attaching all the necessary packages to the vaadin server bundle.

Upgrading to a new version of Vaadin
====================================

Update the deferredjs imports in pom.xml since the numbers seems to change with each version of Vaadin.

    VAADIN.widgetsets.com.vaadin.DefaultWidgetSet.deferredjs.600EB5916BAC256872376F28AF523C79;version="[7.0.3,7.1.0)",
    VAADIN.widgetsets.com.vaadin.DefaultWidgetSet.deferredjs.7F0A4FCD40860D89DBA4A103B3E4C5F4;version="[7.0.3,7.1.0)",
    VAADIN.widgetsets.com.vaadin.DefaultWidgetSet.deferredjs.8C3D06129C9C79D09B6C59E69EA9DF7F;version="[7.0.3,7.1.0)",
    VAADIN.widgetsets.com.vaadin.DefaultWidgetSet.deferredjs.9B6AD845EA493C859908046F21C434F2;version="[7.0.3,7.1.0)",
    VAADIN.widgetsets.com.vaadin.DefaultWidgetSet.deferredjs.C814327B6B0D18983F0BC46834B0F97F;version="[7.0.3,7.1.0)",
    VAADIN.widgetsets.com.vaadin.DefaultWidgetSet.deferredjs.CCD96F3454182FBC42DCFE3485DFF2EC;version="[7.0.3,7.1.0)"
