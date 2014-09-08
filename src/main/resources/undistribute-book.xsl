<?xml version="1.0"?>

<xsl:stylesheet
        version="2.0"
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
        xmlns:undistribute="http://schemas.blinkboxbooks.com/distribution/undistribute/v1"
        >

<xsl:output indent="yes" />
<xsl:strip-space elements="*" />

<xsl:template match="/undistribute:undistribute/undistribute:book">
    <xsl:element name="delete">
        <xsl:attribute name="commitWithin">
            <xsl:text>6000</xsl:text>
        </xsl:attribute>
        <xsl:element name="query">isbn:"<xsl:value-of select="." />"</xsl:element>
    </xsl:element>
</xsl:template>

<xsl:template match="text()" />

</xsl:stylesheet>
