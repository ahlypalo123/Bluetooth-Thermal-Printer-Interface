package com.taviak.printer_interface.data.model

import android.widget.TextView
import androidx.room.*
import java.io.Serializable

typealias Data = MutableMap<String, String?>
typealias ListData = MutableMap<String, MutableList<Data>>

data class Receipt(
    val id: Long? = null,
    val templateData: ReceiptTemplateData? = null,
    val listData: ListData = mutableMapOf(),
    val pictureData: Data = mutableMapOf(),
    val fieldData: Data = mutableMapOf(),
) : Serializable

@Entity
data class ReceiptTemplate(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "templateId")
    val id: Long = 0,
    var active: Boolean = false,
    var data: ReceiptTemplateData = mutableListOf()
) : Serializable

@Entity
data class Variable(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "variableId")
    var id: Long = 0,
    val name: String?,
    var shortName: String?,
    var scope: Int?,
    val field: Int? = VariableFieldType.EDITTEXT.ordinal,
    val valueType: Int? = ValueType.TEXT.ordinal,
    val expression: String? = "",
    val options: List<String> = mutableListOf()
) : Serializable {
    override fun equals(other: Any?): Boolean {
        if (other is Variable) {
            return shortName == other.shortName
        }
        return false
    }

    override fun hashCode(): Int {
        return shortName?.hashCode() ?: 0
    }
}

@Entity(primaryKeys = ["variableId", "templateId"])
data class VariableTemplateCrossRef(
    val variableId: Long,
    val templateId: Long,
)

data class TemplateWithVariables(
    @Embedded val template: ReceiptTemplate,
    @Relation(
        parentColumn = "templateId",
        entityColumn = "variableId",
        associateBy = Junction(VariableTemplateCrossRef::class)
    )
    val variables: List<Variable>
)

interface ReceiptElement {
    val type: String
}

enum class ReceiptElementGroupType {
    LIST, GROUP
}

typealias ReceiptTemplateData = MutableList<MutableList<ReceiptElement>>

data class ReceiptTextElement(
    var text: String,
    var alignment: Int = TextView.TEXT_ALIGNMENT_CENTER,
    var style: MutableList<ReceiptTextStyle> = mutableListOf(),
    var size: ReceiptTextSize = ReceiptTextSize.NORMAL,
    override val type: String = ReceiptTextElement::class.java.name
) : ReceiptElement, Serializable

data class ReceiptListElement(
    var data: ReceiptTemplateData,
    var name: String,
    var groupType: ReceiptElementGroupType,
    override val type: String = ReceiptListElement::class.java.name
) : ReceiptElement, Serializable

data class ReceiptImageElement(
    var fileName: String?,
    var name: String,
    var width: Float = 1F,
    var offset: Float = .5F,
    var imageType: ImageElementType,
    override val type: String = ReceiptImageElement::class.java.name
) : ReceiptElement, Serializable

enum class ImageElementType {
    CONSTANT, VARIABLE
}

enum class ReceiptTextSize(val offset: Int, val nameRes: String) {
    SMALLER(-8, "Мельче"), SMALL(-4, "Мелкий"), NORMAL(0, "Обычный"),
    BIG(4, "Большой"), LARGE(8, "Больше"), LARGER(16, "Огромный")
}

enum class ReceiptTextStyle {
    BOLD, UNDERLINED, ITALIC
}

enum class VariableScope(val value: String) {
    COMMON("Общая"), ITEM("Переменная списка")
}

enum class VariableFieldType(val value: String) {
    EDITTEXT("Текстовое поле"), SPINNER("Выпадающее меню"), MATERIAL_BUTTON("Поле выбора")
}

enum class ValueType(val value: String) {
    TEXT("Текст"), NUMBER("Число"), EXPRESSION("Вычисляемое значение")
}
