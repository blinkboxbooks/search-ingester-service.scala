<?xml version="1.0"?>

<xsl:stylesheet
	version="2.0" 
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:book="http://schemas.blinkbox.com/books/distribution"
>

<xsl:output indent="yes" />

<xsl:strip-space elements="*" />

<xsl:template match="/book:book">
	<xsl:element name="add">
		<xsl:element name="doc">
			<xsl:element name="field">
				<xsl:attribute name="name">isbn</xsl:attribute>
				<xsl:value-of select="book:isbn" />
			</xsl:element>
			
			<xsl:element name="field">
				<xsl:attribute name="name">title</xsl:attribute>
				<xsl:attribute name="update">set</xsl:attribute>
				<xsl:value-of select="book:title" />
			</xsl:element>

			<xsl:for-each select="book:contributors/book:contributor[@role='Author']">
				<xsl:element name="field">
					<xsl:attribute name="name">author</xsl:attribute>
					<xsl:attribute name="update">set</xsl:attribute>
					<xsl:value-of select="book:displayName" />
				</xsl:element>
			</xsl:for-each>
			
			<xsl:element name="field">
				<xsl:attribute name="name">author_sort</xsl:attribute>
				<xsl:attribute name="update">set</xsl:attribute>
				<xsl:for-each select="book:contributors/book:contributor[@role='Author']">
					<xsl:value-of select="book:sortName" />
					<xsl:if test="not(position()=last())">
						<xsl:text>, </xsl:text>
					</xsl:if>
				</xsl:for-each>
			</xsl:element>

            <xsl:for-each select="book:contributors/book:contributor[@role='Author']">
                <xsl:element name="field">
                    <xsl:attribute name="name">author_guid</xsl:attribute>
                    <xsl:attribute name="update">set</xsl:attribute>
                    <xsl:value-of select="@id" />
                </xsl:element>
            </xsl:for-each>

			<xsl:element name="field">
				<xsl:attribute name="name">publication_date</xsl:attribute>
				<xsl:attribute name="update">set</xsl:attribute>
				<xsl:value-of select="book:publishedOn" />
				<xsl:text>T00:00:00Z</xsl:text>
			</xsl:element>
	
			<xsl:element name="field">
				<xsl:attribute name="name">description</xsl:attribute>
				<xsl:attribute name="update">set</xsl:attribute>
				<xsl:text disable-output-escaping="yes">&lt;![CDATA[</xsl:text>
				<xsl:value-of select="book:descriptions/book:description[1]" disable-output-escaping="no" />
				<xsl:text disable-output-escaping="yes">]]&gt;</xsl:text>
			</xsl:element>
	
			<xsl:for-each select="book:subjects/book:subject[@type='Keyword']">
				<xsl:element name="field">
					<xsl:attribute name="update">set</xsl:attribute>
					<xsl:attribute name="name">keyword</xsl:attribute>
					<xsl:value-of select="." />
				</xsl:element>
			</xsl:for-each>
			
			<xsl:for-each select="book:subjects/book:subject[@type='BISAC']">
				<xsl:element name="field">
					<xsl:attribute name="update">set</xsl:attribute>
					<xsl:attribute name="name">bisac_subject</xsl:attribute>
					<xsl:value-of select="." />
				</xsl:element>
			</xsl:for-each>
			
		</xsl:element>
	</xsl:element>
</xsl:template>

<xsl:template match="text()" />

</xsl:stylesheet>
