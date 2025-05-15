package io.github.vinceglb.filekit.dialogs

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.path
import platform.AppKit.NSModalResponseOK
import platform.AppKit.NSOpenPanel
import platform.AppKit.NSSavePanel
import platform.AppKit.allowedFileTypes
import platform.Foundation.NSURL

public actual suspend fun <Out> FileKit.openFilePicker(
    type: FileKitType,
    mode: FileKitMode<Out>,
    title: String?,
    directory: PlatformFile?,
    dialogSettings: FileKitDialogSettings,
    onSelection: ((Int) -> Unit)?
): Out? = callPicker(
    mode = when (mode) {
        is FileKitMode.Single -> Mode.Single
        is FileKitMode.Multiple -> Mode.Multiple
    },
    title = title,
    directory = directory,
    fileExtensions = when (type) {
        FileKitType.Image -> imageExtensions
        FileKitType.Video -> videoExtensions
        FileKitType.ImageAndVideo -> imageExtensions + videoExtensions
        is FileKitType.File -> type.extensions
    },
    dialogSettings = dialogSettings,
)?.map { PlatformFile(it) }?.let { mode.parseResult(it) }

public actual suspend fun FileKit.openDirectoryPicker(
    title: String?,
    directory: PlatformFile?,
    dialogSettings: FileKitDialogSettings,
): PlatformFile? = callPicker(
    mode = Mode.Directory,
    title = title,
    directory = directory,
    fileExtensions = null,
    dialogSettings = dialogSettings,
)?.firstOrNull()?.let { PlatformFile(it) }

public actual suspend fun FileKit.openFileSaver(
    suggestedName: String,
    extension: String?,
    directory: PlatformFile?,
    dialogSettings: FileKitDialogSettings,
): PlatformFile? {
    // Create an NSSavePanel
    val nsSavePanel = NSSavePanel()

    // Set the initial directory
    directory?.let { nsSavePanel.directoryURL = NSURL.fileURLWithPath(it.path) }

    // Set the file name
    nsSavePanel.nameFieldStringValue = when {
        extension != null -> "$suggestedName.$extension"
        else -> suggestedName
    }

    // Set the file extension
    extension?.let {
        nsSavePanel.allowedFileTypes = listOf(extension)
    }

    // Accept the creation of directories
    nsSavePanel.canCreateDirectories = dialogSettings.canCreateDirectories

    // Run the NSSavePanel
    val result = nsSavePanel.runModal()

    // If the user cancelled the operation, return null
    if (result != NSModalResponseOK) {
        return null
    }

    // Return the result
    val platformFile = nsSavePanel.URL?.let { nsUrl ->
        // Create the PlatformFile
        PlatformFile(nsUrl)
    }

    return platformFile
}

private fun callPicker(
    mode: Mode,
    title: String?,
    directory: PlatformFile?,
    fileExtensions: Set<String>?,
    dialogSettings: FileKitDialogSettings,
): List<NSURL>? {
    // Create an NSOpenPanel
    val nsOpenPanel = NSOpenPanel()

    // Configure the NSOpenPanel
    nsOpenPanel.configure(
        mode = mode,
        title = title,
        extensions = fileExtensions,
        directory = directory,
        canCreateDirectories = dialogSettings.canCreateDirectories
    )

    // Run the NSOpenPanel
    val result = nsOpenPanel.runModal()

    // If the user cancelled the operation, return null
    if (result != NSModalResponseOK) {
        return null
    }

    // Return the result
    return nsOpenPanel.URLs.mapNotNull { it as? NSURL }
}

private fun NSOpenPanel.configure(
    mode: Mode,
    title: String?,
    extensions: Set<String>?,
    directory: PlatformFile?,
    canCreateDirectories: Boolean,
): NSOpenPanel {
    // Set the title
    title?.let { message = it }

    // Set the initial directory
    directory?.let { directoryURL = NSURL.fileURLWithPath(it.path) }

    // Set the allowed file types
    extensions?.let { allowedFileTypes = extensions.toList() }

    // Setup the picker mode and files extensions
    when (mode) {
        Mode.Single -> {
            canChooseFiles = true
            canChooseDirectories = false
            allowsMultipleSelection = false
        }

        Mode.Multiple -> {
            canChooseFiles = true
            canChooseDirectories = false
            allowsMultipleSelection = true
        }

        Mode.Directory -> {
            canChooseFiles = false
            canChooseDirectories = true
            allowsMultipleSelection = false
        }
    }

    // Accept the creation of directories
    this.canCreateDirectories = canCreateDirectories

    return this
}

private enum class Mode {
    Single,
    Multiple,
    Directory
}
