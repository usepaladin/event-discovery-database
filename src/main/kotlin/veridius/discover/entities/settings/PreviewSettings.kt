package veridius.discover.entities.settings

import veridius.discover.entities.common.OperationType

data class PreviewSettings(
    // Provide a real time preview of events observed and handled
    var eventPreviewEnabled: Boolean = true,
    // How many events to sample/send to Web UI per second
    var eventPreviewSampleRate: Int = 5,
    // Maximum number of events per table when streaming to Web UI
    var eventPreviewMaxTableEvents: Int = 100,
    // Which operations to send to Web UI
    var eventPreviewOperationTypes: List<OperationType> = listOf(OperationType.CREATE, OperationType.UPDATE),
) {

}