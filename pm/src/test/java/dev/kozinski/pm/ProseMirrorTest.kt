package dev.kozinski.pm

import com.atlassian.prosemirror.model.Node
import com.atlassian.prosemirror.state.EmptyEditorStateConfig
import com.atlassian.prosemirror.state.PMEditorState
import com.atlassian.prosemirror.state.TextSelection
import com.atlassian.prosemirror.transform.findWrapping
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class ProseMirrorTest {
    private val pm = ProseMirror()

    @Test fun `old schema blows up if we leave the default unsupported node or mark config`() {
        val serializedFutureDoc = createDocumentFromFuture().toJSON()

        assertFails("Expected a parsing error") {
            pm.minimalViableSchema.nodeFromJSON(serializedFutureDoc)
        }
    }

    @Test fun `old schema should parse a document that uses a new schema`() {
        val serializedFutureDoc = createDocumentFromFuture().toJSON()

        val compatDoc = pm.minimalForwardCompatibleSchema.nodeFromJSON(serializedFutureDoc, check = true)
        val json = Json { prettyPrint = true }
        assertEquals(
            json.encodeToString(JsonElement.serializer(), serializedFutureDoc),
            json.encodeToString(JsonElement.serializer(), compatDoc.toJSON()),
        )
    }

    private fun createDocumentFromFuture(): Node {
        val futureEmptyState = PMEditorState.create(
                EmptyEditorStateConfig(
                    schema = pm.futureSchema,
                )
            )
        val futureSchema = futureEmptyState.schema
        val futureState = futureEmptyState.transaction {
            // Insert a basic paragraph.
            insertText("Lorem ipsum")

            // Insert a blockquote after it.
            setSelection(TextSelection.create(doc, selection.head + 1))
            val secondParagraph = "dolor sit amet"
            insertText(secondParagraph)
            setSelection(
                TextSelection.create(
                    doc,
                    selection.anchor - secondParagraph.length,
                    selection.anchor
                )
            )
            val range = selection._from.blockRange(selection._to)!!
            wrap(range, findWrapping(range, futureSchema.nodeType("blockquote"))!!)

            // Insert another paragraph with some styling.
            setSelection(TextSelection.create(doc, selection.head + 2))
            insertText("consectetur adipiscing elit")
            addMark(selection.head - 5, selection.head - 1, futureSchema.marks["em"]!!.create())
        }
        return futureState.doc
    }
}
