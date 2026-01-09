package person.cyl.flutterstringextractor

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CustomShortcutSet
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.table.JBTable
import com.jetbrains.lang.dart.psi.DartStringLiteralExpression
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.KeyStroke
import javax.swing.table.AbstractTableModel

data class ExtractAllStringsTableRowData(
    val originalText: String, var key: String, val value: String, val element: DartStringLiteralExpression
)

class ExtractAllStringsDialog(
    project: Project,
    data: List<ExtractAllStringsTableRowData>,
    private val onOk: (List<ExtractAllStringsTableRowData>) -> Unit
) : DialogWrapper(project) {

    // Use a mutable list to back the table model so we can remove items
    private val tableData = data.toMutableList()
    private val tableModel = ExtractAllStringsTableModel(tableData)
    private val table = JBTable(tableModel)
    private val batchUpdateCheckBox = JCheckBox(MyMessageBundle.message("message.batchUpdateKeysForSameValues"))

    init {
        title = "Extract All Strings"
        isModal = false // Make the dialog non-modal
        init()

        // Enable cell selection
        table.setCellSelectionEnabled(true)
        
        // Disable column reordering
        table.tableHeader.reorderingAllowed = false

        // Copy Action
        table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_C, java.awt.Toolkit.getDefaultToolkit().menuShortcutKeyMaskEx), "copy"
        )
        table.actionMap.put("copy", object : javax.swing.AbstractAction() {
            override fun actionPerformed(e: java.awt.event.ActionEvent?) {
                val sbf = StringBuilder()
                val selectedRows = table.selectedRows
                val selectedCols = table.selectedColumns

                for (i in selectedRows.indices) {
                    for (j in selectedCols.indices) {
                        val value = table.getValueAt(selectedRows[i], selectedCols[j])
                        sbf.append(value ?: "")
                        if (j < selectedCols.size - 1) sbf.append("\t")
                    }
                    if (i < selectedRows.size - 1) sbf.append("\n")
                }

                val selection = java.awt.datatransfer.StringSelection(sbf.toString())
                java.awt.Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, selection)
            }
        })

        // Paste Action
        table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_V, java.awt.Toolkit.getDefaultToolkit().menuShortcutKeyMaskEx), "paste"
        )
        table.actionMap.put("paste", object : javax.swing.AbstractAction() {
            override fun actionPerformed(e: java.awt.event.ActionEvent?) {
                val clipboard = java.awt.Toolkit.getDefaultToolkit().systemClipboard
                val content = try {
                    clipboard.getData(java.awt.datatransfer.DataFlavor.stringFlavor) as? String
                } catch (ex: Exception) {
                    null
                } ?: return

                val rows = content.replace("\r\n", "\n").replace("\r", "\n").split("\n")
                // Remove last empty line if exists (common in copy operations)
                val validRows = if (rows.isNotEmpty() && rows.last().isEmpty()) rows.dropLast(1) else rows

                if (validRows.isEmpty()) return

                // Check if we should perform "Fill Selection" (single value pasted into multiple cells)
                val isSingleValue = validRows.size == 1 && validRows[0].split("\t").size == 1
                val isMultiSelection = table.selectedRows.size > 1 || table.selectedColumns.size > 1

                if (isSingleValue && isMultiSelection) {
                    // Paste single value into all selected cells
                    val value = validRows[0].trim()
                    val selectedRows = table.selectedRows
                    val selectedCols = table.selectedColumns

                    for (row in selectedRows) {
                        for (col in selectedCols) {
                            // Ensure we only write to cells that are actually selected and editable
                            if (table.isCellSelected(row, col) && table.isCellEditable(row, col)) {
                                table.setValueAt(value, row, col)
                            }
                        }
                    }
                } else {
                    // Standard paste (overwrite from anchor)
                    val startRow = table.selectedRow
                    val startCol = table.selectedColumn
                    if (startRow == -1 || startCol == -1) return

                    for (i in validRows.indices) {
                        val rowStr = validRows[i]
                        val currentRow = startRow + i
                        if (currentRow >= table.rowCount) break

                        val cells = rowStr.split("\t")
                        for (j in cells.indices) {
                            val currentCol = startCol + j
                            if (currentCol >= table.columnCount) break

                            if (table.isCellEditable(currentRow, currentCol)) {
                                table.setValueAt(cells[j], currentRow, currentCol)
                            }
                        }
                    }
                }
            }
        })

        table.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2) {
                    val row = table.rowAtPoint(e.point)
                    val col = table.columnAtPoint(e.point)
                    // Column 0 is "Original Text"
                    if (row >= 0 && col == 0) {
                        val modelRow = table.convertRowIndexToModel(row)
                        if (modelRow in tableData.indices) {
                            val entry = tableData[modelRow]
                            if (entry.element.isValid) {
                                // Navigate without requesting focus
                                entry.element.navigate(false)
                            }
                        }
                    }
                }
            }
        })
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout())
        panel.preferredSize = Dimension(600, 400)

        // Use ToolbarDecorator to add add/remove actions
        val decorator = ToolbarDecorator.createDecorator(table)
        decorator.setRemoveAction {
            val selectedRows = table.selectedRows
            // Remove from bottom to top to avoid index shifting issues

            if (selectedRows.isNotEmpty()) {
                // Stop editing before removing rows to avoid issues
                if (table.isEditing) {
                    table.cellEditor.stopCellEditing()
                }

                // Sort indices descending
                val indices = selectedRows.sortedDescending()
                for (index in indices) {
                    tableData.removeAt(index)
                }
                tableModel.fireTableDataChanged()
            }
        }
        // Disable Add action as we only want to extract existing strings
        decorator.setAddAction(null)

        val decoratorPanel = decorator.createPanel()
        panel.add(decoratorPanel, BorderLayout.CENTER)

        // Add checkbox at the bottom
        val bottomPanel = JPanel(BorderLayout())
        bottomPanel.add(batchUpdateCheckBox, BorderLayout.WEST)
        panel.add(bottomPanel, BorderLayout.SOUTH)

        // Register Backspace action using IntelliJ's action system to override ToolbarDecorator's default
        val clearAction = object : AnAction() {
            init {
                // Important: Disable in modal context to allow editor to handle it when editing
                isEnabledInModalContext = true
            }

            override fun actionPerformed(e: AnActionEvent) {
                // If the table is currently editing, we MUST NOT consume the event or do anything.
                // We should let the editor component handle the Backspace key.
                if (table.isEditing) {
                    return
                }

                val selectedRows = table.selectedRows
                val selectedCols = table.selectedColumns

                for (row in selectedRows) {
                    for (col in selectedCols) {
                        if (table.isCellSelected(row, col) && table.isCellEditable(row, col)) {
                            table.setValueAt("", row, col)
                        }
                    }
                }
            }

            override fun update(e: AnActionEvent) {
                // Disable this action if the table is in editing mode
                // This allows the key event to fall through to the active editor component
                e.presentation.isEnabled = !table.isEditing
            }
        }
        clearAction.registerCustomShortcutSet(
            CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0)),
            table
        )

        return panel
    }

    override fun doOKAction() {
        // Stop editing when OK is clicked to ensure the last edit is saved
        if (table.isEditing) {
            table.cellEditor.stopCellEditing()
        }

        // Validate data: Check for empty keys
        val emptyKeyEntries = tableData.filter { it.key.isBlank() }
        if (emptyKeyEntries.isNotEmpty()) {
            Messages.showErrorDialog(
                MyMessageBundle.message("error.existEntryWithEmptyKey", emptyKeyEntries.size),
                MyMessageBundle.message("title.validationError")
            )
            return
        }

        // Execute the callback with the current data
        onOk(tableData)
        super.doOKAction()
    }

    // Custom Table Model
    private inner class ExtractAllStringsTableModel(private val entries: List<ExtractAllStringsTableRowData>) :
        AbstractTableModel() {
        override fun getRowCount(): Int = entries.size

        override fun getColumnCount(): Int = 3

        override fun getColumnName(column: Int): String = when (column) {
            0 -> "Original Text"
            1 -> "Key"
            2 -> "Value"
            else -> ""
        }

        override fun getValueAt(row: Int, column: Int): Any = when (column) {
            0 -> entries[row].originalText
            1 -> entries[row].key
            2 -> entries[row].value
            else -> ""
        }

        override fun setValueAt(aValue: Any?, row: Int, column: Int) {
            if (column == 1 && aValue is String) {
                entries[row].key = aValue
                fireTableCellUpdated(row, column)

                if (batchUpdateCheckBox.isSelected) {
                    val targetValue = entries[row].value
                    for (i in entries.indices) {
                        if (i != row && entries[i].value == targetValue) {
                            entries[i].key = aValue
                            fireTableCellUpdated(i, column)
                        }
                    }
                }
            }
        }

        override fun isCellEditable(row: Int, column: Int): Boolean = column == 1 // Only Key is editable
    }
}