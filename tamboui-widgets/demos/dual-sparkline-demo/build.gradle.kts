plugins {
    id("dev.tamboui.demo-project")
}

description = "Demo showcasing the DualSparkline widget"

demo {
    tags = setOf("sparkline", "dual-sparkline", "block", "paragraph", "data-visualization", "animation")
}

application {
    mainClass.set("dev.tamboui.demo.DualSparklineDemo")
}
