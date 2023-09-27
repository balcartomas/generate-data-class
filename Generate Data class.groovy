import com.intellij.database.model.DasTable
import com.intellij.database.util.Case
import com.intellij.database.util.DasUtil

/*
 * Available context bindings:
 *   SELECTION   Iterable<DasObject>
 *   PROJECT     project
 *   FILES       files helper
 */

typeMapping = [
        (~/(?i)serial|long/)        : ["Long", ""],
        (~/(?i)int/)                : ["Int", ""],
        (~/(?i)smallint/)           : ["Short", ""],
        (~/(?i)float/)              : ["Float", ""],
        (~/(?i)double|decimal|real/): ["Double", ""],
        (~/(?i)datetime|timestamp/) : ["Timestamp", "java.sql.Timestamp"],
        (~/(?i)date/)               : ["Date", "java.sql.Date"],
        (~/(?i)time/)               : ["Time", "java.sql.Time"],
        (~/(?i)/)                   : ["String", ""]
]

FILES.chooseDirectoryAndSave("Choose directory", "Choose where to store generated files") { dir ->
    SELECTION.filter { it instanceof DasTable }.each { generate(it, dir) }
}

def generate(table, dir) {
    def className = kotlinName(table.getName(), true)
    def fields = calcFields(table)
    def packageName = createPackageName(dir)
    new File(dir, className + ".kt").withPrintWriter { out ->
        generate(out, className, fields, packageName)
    }
}

def generate(out, className, fields, packageName) {
    out.println "package $packageName"
    out.println ""
    fields.each() {
        if (!it.import.toString().isBlank()) {
            out.println "import ${it.import}"
        }
    }

    out.println ""
    out.println "data class $className("
    fields.each() {
        if (it.annos != "") out.println "  ${it.annos}"
        out.println "    val ${it.name}: ${it.type},"
    }
    out.println ")"
}

def createPackageName(dir) {
    def separator = "\\\\"
    def arrayOfWords = dir.absolutePath.split(separator).findAll { it != "" }

    def isBehindKotlin = false
    def packageName = ""
    arrayOfWords.each() {
        if (it.toString().toLowerCase() == "kotlin") {
            isBehindKotlin = true
        } else {
            if (isBehindKotlin == true) {
                if (packageName == "") {
                    packageName = it.toString()
                } else {
                    packageName += ".${it.toString()}"
                }
            }
        }
    }

    return packageName
}

def calcFields(table) {
    DasUtil.getColumns(table).reduce([]) { fields, col ->
        def spec = Case.LOWER.apply(col.getDasType().getSpecification())
        def typeArray = typeMapping.find { p, t ->
            if (p.matcher(spec).asBoolean()) {
                t
            }
        }.value
        def typeStr = typeArray[0].toString()
        def importStr = ""
        if (typeArray[1] != null && !typeArray[1].toString().isBlank()) {
            importStr = typeArray[1]
        }

        fields += [[
                           name  : kotlinName(col.getName(), false),
                           type  : typeStr,
                           import: importStr,
                           annos : ""]]
    }
}

def kotlinName(str, capitalize) {
    def s = com.intellij.psi.codeStyle.NameUtil.splitNameIntoWords(str)
            .collect { Case.LOWER.apply(it).capitalize() }
            .join("")
    capitalize || s.length() == 1 ? s : Case.LOWER.apply(s[0]) + s[1..-1]
}
