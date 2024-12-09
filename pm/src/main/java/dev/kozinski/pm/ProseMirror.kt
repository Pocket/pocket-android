package dev.kozinski.pm

import com.atlassian.prosemirror.model.AttributeSpec
import com.atlassian.prosemirror.model.Attrs
import com.atlassian.prosemirror.model.DOMOutputSpec
import com.atlassian.prosemirror.model.Fragment
import com.atlassian.prosemirror.model.Mark
import com.atlassian.prosemirror.model.MarkCreator
import com.atlassian.prosemirror.model.MarkType
import com.atlassian.prosemirror.model.MarkSpec as AbstractMarkSpec
import com.atlassian.prosemirror.model.Node
import com.atlassian.prosemirror.model.NodeCreator
import com.atlassian.prosemirror.model.NodeType
import com.atlassian.prosemirror.model.NodeSpec as AbstractNodeSpec
import com.atlassian.prosemirror.model.ParseRule
import com.atlassian.prosemirror.model.Schema
import com.atlassian.prosemirror.model.SchemaSpec
import com.atlassian.prosemirror.model.TagParseRule
import com.atlassian.prosemirror.model.UnsupportedMark
import com.atlassian.prosemirror.model.UnsupportedNode
import com.atlassian.prosemirror.model.Whitespace
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.put

class ProseMirror {
    val minimalViableSchema = Schema(
        SchemaSpec(
            nodes = mapOf(
                "doc" to NodeSpec(
                    content = "block+",
                ),
                "paragraph" to NodeSpec(
                    content = "inline*",
                    group = "block",
                ),
                "text" to NodeSpec(
                    group = "inline",
                    inline = true,
                    selectable = false,
                ),
            ),
        )
    )

    val minimalForwardCompatibleSchema = Schema(
        SchemaSpec(
            nodes = minimalViableSchema.spec.nodes.plus(
                mapOf(
                    "unsupportedBlock" to NodeSpec(
                        content = "block+ | inline*",
                        group = "block"
                    ),
                    "unsupportedInline" to NodeSpec(
                        group = "inline",
                        inline = true,
                        selectable = false,
                    )
                )
            ),
            marks = minimalViableSchema.spec.marks.plus(
                mapOf(
                    "unsupportedMark" to MarkSpec(),
                )
            ),
        )
    ).apply {
        val unsupportedNodeCreator = object : NodeCreator<Node> {
            override fun create(
                type: NodeType,
                attrs: Attrs,
                content: Fragment?,
                marks: List<Mark>,
            ) = FutureNode(type, attrs, content, marks)
        }
        nodes["unsupportedBlock"]?.creator = unsupportedNodeCreator
        nodes["unsupportedInline"]?.creator = unsupportedNodeCreator

        val unsupportedMarkCreator: MarkCreator<Mark> = object : MarkCreator<Mark> {
            override fun create(type: MarkType, attrs: Attrs) = FutureMark(type, attrs)
        }
        marks["unsupportedMark"]?.creator = unsupportedMarkCreator
    }

    val futureSchema = Schema(
        SchemaSpec(
            nodes = minimalForwardCompatibleSchema.spec.nodes.plus(
                mapOf(
                    "blockquote" to NodeSpec(
                        content = "block+",
                        group = "block",
                        defining = true,
                    ),
                    "image" to NodeSpec(
                        inline = true,
                        attrs = mapOf(
                            "src" to object : AttributeSpec {
                                override val default: Any? = null
                                override val hasDefault = false
                                override val validateFunction: ((value: Any?) -> Unit)? = null
                                override val validateString = "string"
                            }
                        ),
                    )
                )
            ),
            marks = minimalForwardCompatibleSchema.spec.marks.plus(
                mapOf(
                    "em" to MarkSpec(),
                )
            )
        )
    )
}

data class NodeSpec(
    // The content expression for this node, as described in the
    // [schema guide](/docs/guide/#schema.content_expressions). When not given, the node does not
    // allow any content.
    override val content: String? = null,

    // The marks that are allowed inside of this node. May be a space-separated string referring to
    // mark names or groups, `"_"` to explicitly allow all marks, or `""` to disallow marks. When
    // not given, nodes with inline content default to allowing all marks, other nodes default to
    // not allowing marks.
    override val marks: String? = null,

    // The group or space-separated groups to which this node belongs, which can be referred to in
    // the content expressions for the schema.
    override val group: String? = null,

    // Should be set to true for inline nodes. (Implied for text nodes.)
    override val inline: Boolean = !content.isNullOrBlank(),

    // Can be set to true to indicate that, though this isn't a [leaf node](#model.NodeType.isLeaf),
    // it doesn't have directly editable content and should be treated as a single unit in the view.
    override val atom: Boolean = false,

    // The attributes that nodes of this type get.
    override val attrs: Map<String, AttributeSpec>? = null,

    // Controls whether nodes of this type can be selected as a
    // [node selection](#state.NodeSelection). Defaults to true for non-text nodes.
    override val selectable: Boolean = true,

    // Determines whether nodes of this type can be dragged without being selected.
    // Defaults to false.
    override val draggable: Boolean = false,

    // Can be used to indicate that this node contains code, which causes some commands to behave
    // differently.
    override val code: Boolean = false,

    // Controls way whitespace in this a node is parsed. The default is `"normal"`, which causes the
    // [DOM parser](#model.DOMParser) to collapse whitespace in normal mode, and normalize it
    // (replacing newlines and such with spaces) otherwise. `"pre"` causes the parser to preserve
    // spaces inside the node. When this option isn't given, but [`code`](#model.NodeSpec.code) is
    // true, `whitespace` will default to `"pre"`. Note that this option doesn't influence the way
    // the node is rendered—that should be handled by `toDOM` and/or styling.
    override val whitespace: Whitespace? = null,

    // Determines whether this node is considered an important parent node during replace operations
    // (such as paste). Non-defining (the default) nodes get dropped when their entire content is
    // replaced, whereas defining nodes persist and wrap the inserted content.
    override val definingAsContext: Boolean? = null,

    // In inserted content the defining parents of the content are preserved when possible.
    // Typically, non-default-paragraph textblock types, and possibly list items, are marked as
    // defining.
    override val definingForContent: Boolean? = null,

    // When enabled, enables both [`definingAsContext`](#model.NodeSpec.definingAsContext) and
    // [`definingForContent`](#model.NodeSpec.definingForContent).
    override val defining: Boolean? = null,

    // When enabled (default is false), the sides of nodes of this type count as boundaries that
    // regular editing operations, like backspacing or lifting, won't cross. An example of a node
    // that should probably have this enabled is a table cell.
    override val isolating: Boolean? = null,

    // Defines the default way a node of this type should be serialized to DOM/HTML (as used by
    // [`DOMSerializer.fromSchema`](#model.DOMSerializer^fromSchema)). Should return a DOM node or
    // an [array structure](#model.DOMOutputSpec) that describes one, with an optional number zero
    // (“hole”) in it to indicate where the node's content should be inserted.
    //
    // For text nodes, the default is to create a text DOM node. Though it is possible to create a
    // serializer where text is rendered differently, this is not supported inside the editor, so
    // you shouldn't override that in your text node spec.
    override val toDOM: ((node: Node) -> DOMOutputSpec)? = null,

    // Associates DOM parser information with this node, which can be used by
    // [`DOMParser.fromSchema`](#model.DOMParser^fromSchema) to automatically derive a parser. The
    // `node` field in the rules is implied (the name of this node will be filled in automatically).
    // If you supply your own parser, you do not need to also specify parsing rules in your schema.
    override val parseDOM: List<TagParseRule>? = null,

    // Defines the default way a node of this type should be serialized to a string representation
    // for debugging (e.g. in error messages).
    override val toDebugString: ((node: Node) -> String)? = null,

    // Defines the default way a [leaf node](#model.NodeType.isLeaf) of this type should be
    // serialized to a string (as used by [`Node.textBetween`](#model.Node^textBetween) and
    // [`Node.textContent`](#model.Node^textContent)).
    override val leafText: ((node: Node) -> String)? = null,

    // A single inline node in a schema can be set to be a linebreak
    // equivalent. When converting between block types that support the
    // node and block types that don't but have
    // [`whitespace`](#model.NodeSpec.whitespace) set to `"pre"`,
    // [`setBlockType`](#transform.Transform.setBlockType) will convert
    // between newline characters to or from linebreak nodes as
    // appropriate.
    override val linebreakReplacement: Boolean? = null,

    // Determines whether this node is automatically focused during navigation. Mainly used for navigation with arrow
    // key and backspace/delete key. Defaults to false.
    override val autoFocusable: Boolean? = null,
) : AbstractNodeSpec

data class MarkSpec(
    // The attributes that marks of this type get.
    override val attrs: Map<String, AttributeSpec>? = null,

    // Whether this mark should be active when the cursor is positioned at its end (or at its start
    // when that is also the start of the parent node). Defaults to true.
    override val inclusive: Boolean? = null,

    // Determines which other marks this mark can coexist with. Should be a space-separated strings
    // naming other marks or groups of marks. When a mark is [added](#model.Mark.addToSet) to a set,
    // all marks that it excludes are removed in the process. If the set contains any mark that
    // excludes the new mark but is not, itself, excluded by the new mark, the mark can not be added
    // an the set. You can use the value `"_"` to indicate that the mark excludes all marks in the
    // schema.
    //
    // Defaults to only being exclusive with marks of the same type. You can set it to an empty
    // string (or any string not containing the mark's own name) to allow multiple marks of a given
    // type to coexist (as long as they have different attributes).
    override val excludes: String? = null,

    // The group or space-separated groups to which this mark belongs.
    override val group: String? = null,

    // Determines whether marks of this type can span multiple adjacent nodes when serialized to
    // DOM/HTML. Defaults to true.
    override val spanning: Boolean? = null,

    // Defines the default way marks of this type should be serialized to DOM/HTML. When the
    // resulting spec contains a hole, that is where the marked content is placed. Otherwise, it is
    // appended to the top node.
    override val toDOM: ((mark: Mark, inline: Boolean) -> DOMOutputSpec)? = null,

    // Associates DOM parser information with this mark (see the corresponding
    // [node spec field](#model.NodeSpec.parseDOM)). The `mark` field in the rules is implied.
    override val parseDOM: List<ParseRule>? = null,
) : AbstractMarkSpec

class FutureNode(
    type: NodeType,
    attrs: Attrs,
    content: Fragment?,
    marks: List<Mark>,
) : Node(type, attrs, content, marks), UnsupportedNode {
    override var originalNodeName: String? = null

    override fun toJson(
        builder: JsonObjectBuilder,
        withId: Boolean
    ): JsonObjectBuilder {
        return super.toJson(builder, withId).apply {
            originalNodeName?.let { put("type", it) }
        }
    }
}

class FutureMark(type: MarkType, attrs: Attrs) : Mark(type, attrs), UnsupportedMark {
    override var originalMarkName: String? = null

    override fun toJson(
        builder: JsonObjectBuilder,
        withId: Boolean
    ): JsonObjectBuilder {
        return super.toJson(builder, withId).apply {
            originalMarkName?.let { put("type", it) }
        }
    }
}
