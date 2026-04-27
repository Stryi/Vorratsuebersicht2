package de.stryi.vorratsuebersicht2.tools

fun String.trimEnd(text: String) : String
{
    if (this.endsWith(text))
    {
        return  this.substring(0, this.length - text.length)
    }
    return  this
}
