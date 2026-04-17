package mx.visionebc.actorstoolkit.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.json.JSONArray
import org.json.JSONObject

@Entity(tableName = "projects")
data class Project(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val castingDirector: String = "",
    val director: String = "",
    val notes: String = "",
    val direction: String = "",
    val startDate: Long? = null,
    val endDate: Long? = null,
    val auditionDatesJson: String = "[]",
    val presentationDatesJson: String = "[]",
    val linksJson: String = "[]",
    val attachmentsJson: String = "[]",
    val teamJson: String = "[]",
    val locationsJson: String = "[]",
    val cardColor: String = "",        // hex color e.g. "#FF7B4DFF"
    val cardImageUri: String = "",     // URI to user-picked image
    val sortOrder: Int = 0,
    val shareId: String = java.util.UUID.randomUUID().toString(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

// ── Data classes ──

data class AuditionDateEntry(
    val date: Long,
    val label: String = "",
    val documentUrl: String = ""
)

data class ImportantDateEntry(
    val date: Long,                  // start date epoch millis
    val endDate: Long? = null,       // end date for ranges / series end
    val label: String = "",
    val hasTime: Boolean = false,
    val recurrence: String = "NONE", // NONE, DAILY, WEEKLY_MON..SUN, BIWEEKLY_MON..SUN
    val startHour: Int = -1,         // recurring: start hour (0-23), -1 = not set
    val startMinute: Int = 0,        // recurring: start minute
    val endHour: Int = -1,           // recurring: end hour, -1 = not set
    val endMinute: Int = 0           // recurring: end minute
)

// Keep backward compat alias
typealias PresentationDateEntry = ImportantDateEntry

data class LinkEntry(
    val url: String,
    val label: String = "",
    val note: String = ""
)

data class AttachmentEntry(
    val uri: String,
    val name: String = ""
)

data class TeamMemberEntry(
    val name: String,
    val role: String = ""
)

data class LocationEntry(
    val type: String = "",    // Rehearsal, Presentation, Casting, Other
    val address: String = "",
    val label: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null
)

// ── JSON converters ──

object ProjectJsonConverter {

    // Audition dates
    fun auditionDatesToJson(entries: List<AuditionDateEntry>): String {
        val arr = JSONArray()
        entries.forEach { e -> arr.put(JSONObject().apply { put("date", e.date); put("label", e.label); put("documentUrl", e.documentUrl) }) }
        return arr.toString()
    }

    fun auditionDatesFromJson(json: String): List<AuditionDateEntry> = try {
        val arr = JSONArray(json)
        (0 until arr.length()).map { i -> val o = arr.getJSONObject(i); AuditionDateEntry(o.getLong("date"), o.optString("label", ""), o.optString("documentUrl", "")) }
    } catch (_: Exception) { emptyList() }

    // Important dates (with optional time, end date, recurrence, and hours)
    fun presentationDatesToJson(entries: List<ImportantDateEntry>): String {
        val arr = JSONArray()
        entries.forEach { e -> arr.put(JSONObject().apply {
            put("date", e.date); put("label", e.label); put("hasTime", e.hasTime); put("recurrence", e.recurrence)
            put("startHour", e.startHour); put("startMinute", e.startMinute)
            put("endHour", e.endHour); put("endMinute", e.endMinute)
            if (e.endDate != null) put("endDate", e.endDate)
        }) }
        return arr.toString()
    }

    fun presentationDatesFromJson(json: String): List<ImportantDateEntry> = try {
        val arr = JSONArray(json)
        (0 until arr.length()).map { i -> val o = arr.getJSONObject(i)
            ImportantDateEntry(
                o.getLong("date"), if (o.has("endDate")) o.getLong("endDate") else null,
                o.optString("label", ""), o.optBoolean("hasTime", false), o.optString("recurrence", "NONE"),
                o.optInt("startHour", -1), o.optInt("startMinute", 0),
                o.optInt("endHour", -1), o.optInt("endMinute", 0)
            )
        }
    } catch (_: Exception) { emptyList() }

    // Links
    fun linksToJson(entries: List<LinkEntry>): String {
        val arr = JSONArray()
        entries.forEach { e -> arr.put(JSONObject().apply { put("url", e.url); put("label", e.label); put("note", e.note) }) }
        return arr.toString()
    }

    fun linksFromJson(json: String): List<LinkEntry> = try {
        val arr = JSONArray(json)
        (0 until arr.length()).map { i -> val o = arr.getJSONObject(i); LinkEntry(o.getString("url"), o.optString("label", ""), o.optString("note", "")) }
    } catch (_: Exception) { emptyList() }

    // Attachments
    fun attachmentsToJson(entries: List<AttachmentEntry>): String {
        val arr = JSONArray()
        entries.forEach { e -> arr.put(JSONObject().apply { put("uri", e.uri); put("name", e.name) }) }
        return arr.toString()
    }

    fun attachmentsFromJson(json: String): List<AttachmentEntry> = try {
        val arr = JSONArray(json)
        (0 until arr.length()).map { i -> val o = arr.getJSONObject(i); AttachmentEntry(o.getString("uri"), o.optString("name", "")) }
    } catch (_: Exception) { emptyList() }

    // Team members
    fun teamToJson(entries: List<TeamMemberEntry>): String {
        val arr = JSONArray()
        entries.forEach { e -> arr.put(JSONObject().apply { put("name", e.name); put("role", e.role) }) }
        return arr.toString()
    }

    fun teamFromJson(json: String): List<TeamMemberEntry> = try {
        val arr = JSONArray(json)
        (0 until arr.length()).map { i -> val o = arr.getJSONObject(i); TeamMemberEntry(o.getString("name"), o.optString("role", "")) }
    } catch (_: Exception) { emptyList() }

    // Locations
    fun locationsToJson(entries: List<LocationEntry>): String {
        val arr = JSONArray()
        entries.forEach { e -> arr.put(JSONObject().apply {
            put("type", e.type); put("address", e.address); put("label", e.label)
            if (e.latitude != null) put("latitude", e.latitude)
            if (e.longitude != null) put("longitude", e.longitude)
        }) }
        return arr.toString()
    }

    fun locationsFromJson(json: String): List<LocationEntry> = try {
        val arr = JSONArray(json)
        (0 until arr.length()).map { i -> val o = arr.getJSONObject(i)
            LocationEntry(o.optString("type", ""), o.optString("address", ""), o.optString("label", ""),
                if (o.has("latitude")) o.getDouble("latitude") else null,
                if (o.has("longitude")) o.getDouble("longitude") else null)
        }
    } catch (_: Exception) { emptyList() }
}

// Keep backward compat alias
object AuditionDateConverter {
    fun toJson(entries: List<AuditionDateEntry>) = ProjectJsonConverter.auditionDatesToJson(entries)
    fun fromJson(json: String) = ProjectJsonConverter.auditionDatesFromJson(json)
}
