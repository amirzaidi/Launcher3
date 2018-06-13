package com.android.launcher3.graphics;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

public class XmlPullParserShim implements XmlPullParser {
    private XmlPullParser mInternal;
    private int mStartDepth;

    public XmlPullParserShim(XmlPullParser internal) {
        mInternal = internal;
        mStartDepth = getDepth();
    }

    @Override
    public void setFeature(String name, boolean state) throws XmlPullParserException {
        mInternal.setFeature(name, state);
    }

    @Override
    public boolean getFeature(String name) {
        return mInternal.getFeature(name);
    }

    @Override
    public void setProperty(String name, Object value) throws XmlPullParserException {
        mInternal.setProperty(name, value);
    }

    @Override
    public Object getProperty(String name) {
        return mInternal.getProperty(name);
    }

    @Override
    public void setInput(Reader in) throws XmlPullParserException {
        mInternal.setInput(in);
    }

    @Override
    public void setInput(InputStream inputStream, String inputEncoding) throws XmlPullParserException {
        mInternal.setInput(inputStream, inputEncoding);
    }

    @Override
    public String getInputEncoding() {
        return mInternal.getInputEncoding();
    }

    @Override
    public void defineEntityReplacementText(String entityName, String replacementText) throws XmlPullParserException {
        mInternal.defineEntityReplacementText(entityName, replacementText);
    }

    @Override
    public int getNamespaceCount(int depth) throws XmlPullParserException {
        return mInternal.getNamespaceCount(depth);
    }

    @Override
    public String getNamespacePrefix(int pos) throws XmlPullParserException {
        return mInternal.getNamespacePrefix(pos);
    }

    @Override
    public String getNamespaceUri(int pos) throws XmlPullParserException {
        return mInternal.getNamespaceUri(pos);
    }

    @Override
    public String getNamespace(String prefix) {
        return mInternal.getNamespace(prefix);
    }

    @Override
    public int getDepth() {
        return mInternal.getDepth();
    }

    @Override
    public String getPositionDescription() {
        return mInternal.getPositionDescription();
    }

    @Override
    public int getLineNumber() {
        return mInternal.getLineNumber();
    }

    @Override
    public int getColumnNumber() {
        return mInternal.getColumnNumber();
    }

    @Override
    public boolean isWhitespace() throws XmlPullParserException {
        return mInternal.isWhitespace();
    }

    @Override
    public String getText() {
        return mInternal.getText();
    }

    @Override
    public char[] getTextCharacters(int[] holderForStartAndLength) {
        return mInternal.getTextCharacters(holderForStartAndLength);
    }

    @Override
    public String getNamespace() {
        return mInternal.getNamespace();
    }

    @Override
    public String getName() {
        return mInternal.getName();
    }

    @Override
    public String getPrefix() {
        return mInternal.getPrefix();
    }

    @Override
    public boolean isEmptyElementTag() throws XmlPullParserException {
        return mInternal.isEmptyElementTag();
    }

    @Override
    public int getAttributeCount() {
        return mInternal.getAttributeCount();
    }

    @Override
    public String getAttributeNamespace(int index) {
        return mInternal.getAttributeNamespace(index);
    }

    @Override
    public String getAttributeName(int index) {
        return mInternal.getAttributeName(index);
    }

    @Override
    public String getAttributePrefix(int index) {
        return mInternal.getAttributePrefix(index);
    }

    @Override
    public String getAttributeType(int index) {
        return mInternal.getAttributeType(index);
    }

    @Override
    public boolean isAttributeDefault(int index) {
        return mInternal.isAttributeDefault(index);
    }

    @Override
    public String getAttributeValue(int index) {
        return mInternal.getAttributeValue(index);
    }

    @Override
    public String getAttributeValue(String namespace, String name) {
        return mInternal.getAttributeValue(namespace, name);
    }

    @Override
    public int getEventType() throws XmlPullParserException {
        return mInternal.getEventType();
    }

    @Override
    public int next() throws XmlPullParserException, IOException {
        int type = mInternal.next();
        if (getDepth() == mStartDepth) {
            return XmlPullParser.END_DOCUMENT;
        }
        return type;
    }

    @Override
    public int nextToken() throws XmlPullParserException, IOException {
        return mInternal.nextToken();
    }

    @Override
    public void require(int type, String namespace, String name) throws XmlPullParserException, IOException {
        mInternal.require(type, namespace, name);
    }

    @Override
    public String nextText() throws XmlPullParserException, IOException {
        return mInternal.nextText();
    }

    @Override
    public int nextTag() throws XmlPullParserException, IOException {
        return mInternal.nextTag();
    }
}
