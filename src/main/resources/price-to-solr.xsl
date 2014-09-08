<?xml version="1.0"?>

<xsl:stylesheet
	version="2.0" 
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:p="http://schemas.blinkbox.com/books/pricing"
>

<xsl:output indent="yes" />

<xsl:strip-space elements="*" />  

<xsl:template match="/p:book-price">
	<xsl:element name="add">
		<xsl:element name="doc">
			<xsl:element name="field">
				<xsl:attribute name="name">isbn</xsl:attribute>
				<xsl:value-of select="p:isbn" />
			</xsl:element>
			
			<xsl:element name="field">
				<xsl:attribute name="name">price</xsl:attribute>
				<xsl:attribute name="update">set</xsl:attribute>
				<xsl:value-of select="p:price" />
			</xsl:element>
		</xsl:element>
	</xsl:element>
</xsl:template>

<xsl:template match="text()" />

</xsl:stylesheet>
