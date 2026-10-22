package app.tada.patches.instagram.layout.branding

import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.patch.stringOption
import app.morphe.util.asSequence
import app.morphe.util.removeFromParent
import app.tada.patches.instagram.shared.Constants.COMPATIBILITY_INSTAGRAM
import org.w3c.dom.Element
import java.io.FileWriter
import java.nio.file.Files

private const val DEFAULT_APP_NAME = "TADa Instagram"

@Suppress("unused")
val customBrandingNamePatch = resourcePatch(
    name = "Custom branding name for Instagram",
    description = "Changes the Instagram app name to TADa Instagram (or a custom name specified in patch options).",
    default = true
) {
    compatibleWith(COMPATIBILITY_INSTAGRAM)

    val appNameOption = stringOption(
        key = "appName",
        default = DEFAULT_APP_NAME,
        values = mapOf(
            "Default" to DEFAULT_APP_NAME
        ),
        title = "App name",
        description = "The name of the app displayed on launcher and system settings.",
        required = true
    )

    execute {
        val appName = appNameOption.value ?: DEFAULT_APP_NAME
        val resourceNames = mutableListOf("app_name", "instagram")

        // 1. Process AndroidManifest.xml
        document("AndroidManifest.xml").use { document ->
            val application = document.getElementsByTagName("application").item(0) as? Element
            if (application != null) {
                val label = application.getAttribute("android:label")
                if (label.startsWith("@string/")) {
                    resourceNames += label.substring("@string/".length)
                } else if (label.isNotEmpty()) {
                    application.setAttribute("android:label", appName)
                }
            }

            arrayOf("activity", "activity-alias").forEach { tagName ->
                val elements = document.getElementsByTagName(tagName)
                for (index in 0 until elements.length) {
                    val element = elements.item(index) as? Element ?: continue
                    val label = element.getAttribute("android:label")
                    if (label.startsWith("@string/")) {
                        resourceNames += label.substring("@string/".length)
                    } else if (label.isNotEmpty()) {
                        element.setAttribute("android:label", appName)
                    }
                }
            }
        }

        val distinctResourceNames = resourceNames.distinct()

        // 2. Process all res/values* folders
        val resDir = get("res")
        val valuesDirs = resDir.listFiles { file ->
            file.isDirectory && (file.name == "values" || file.name.startsWith("values-"))
        }?.toList() ?: emptyList()

        for (dir in valuesDirs) {
            val stringsXml = dir.resolve("strings.xml")
            if (!stringsXml.exists()) continue

            val isDefaultValues = dir.name == "values" || dir.name == "values-v24"
            document("res/${dir.name}/strings.xml").use { document ->
                val resources = document.getElementsByTagName("resources").item(0) as? Element ?: return@use
                val declarations = resources.childNodes.asSequence()
                    .filterIsInstance<Element>()
                    .filter { it.tagName == "string" && it.getAttribute("name") in distinctResourceNames }
                    .toList()

                if (!isDefaultValues) {
                    // Remove localized overrides so launcher falls back to default values
                    declarations.forEach { it.removeFromParent() }
                } else {
                    val existingNames = declarations.map { it.getAttribute("name") }.toSet()
                    declarations.forEach { it.textContent = appName }

                    distinctResourceNames.filter { it !in existingNames }.forEach { resName ->
                        val stringElement = document.createElement("string")
                        stringElement.setAttribute("name", resName)
                        stringElement.textContent = appName
                        resources.appendChild(stringElement)
                    }
                }
            }
        }

        // 3. Ensure values-v24/strings.xml exists and contains the string overrides
        val valuesV24Directory = resDir.resolve("values-v24")
        if (!valuesV24Directory.isDirectory) {
            Files.createDirectories(valuesV24Directory.toPath())
        }

        val v24StringsXml = valuesV24Directory.resolve("strings.xml")
        if (!v24StringsXml.exists()) {
            FileWriter(v24StringsXml).use {
                it.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources>\n</resources>")
            }
        }

        document("res/values-v24/strings.xml").use { document ->
            val resourcesNode = document.getElementsByTagName("resources").item(0) as? Element ?: return@use
            val existing = resourcesNode.childNodes.asSequence()
                .filterIsInstance<Element>()
                .filter { it.tagName == "string" && it.getAttribute("name") in distinctResourceNames }
                .associateBy { it.getAttribute("name") }

            distinctResourceNames.forEach { resName ->
                val elem = existing[resName]
                if (elem != null) {
                    elem.textContent = appName
                } else {
                    val stringElement = document.createElement("string")
                    stringElement.setAttribute("name", resName)
                    stringElement.textContent = appName
                    resourcesNode.appendChild(stringElement)
                }
            }
        }
    }
}

