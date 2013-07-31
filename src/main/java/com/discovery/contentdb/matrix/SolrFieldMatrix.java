package com.discovery.contentdb.matrix;

import com.google.common.collect.Maps;
import org.apache.mahout.cf.taste.impl.common.FastIDSet;
import org.apache.mahout.math.*;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrResponse;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.ContentStream;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

/**
 * {@author} gcapan
 * Casts a Solr Field to a read-only matrix, where one document represents a row, and indexed by the idField.
 */
public class SolrFieldMatrix extends AbstractMatrix {
  private String idField;
  private String field;
  private TYPE type;
  private HttpSolrServer server;
  private int rows;
  private String[] columnLabels;
  private Map<String, Integer> columnLabelBindings;

  public SolrFieldMatrix(String url){
    super(0,0);
    this.server = new HttpSolrServer(url);
    server.setMaxRetries(1);
    server.setConnectionTimeout(2000);
    server.setAllowCompression(true);
    initialize();
  }

  private void initialize(){
    int columns = 0;
    columnLabelBindings = Maps.newHashMap();
    if(type.equals(TYPE.BOOLEAN)|| type.equals(TYPE.NUMERICAL)){
      columnLabelBindings.put(field, 0);
      columns = 1;
    } else if(type.equals(TYPE.MULTINOMIAL)){
      /*TODO: Fill the columnLabelBindings with <uniqueValue, autoIncrementArrayIndex> pairs. This way,
      we can return a SparseVector when a row is asked, by fetching column ids from columnLabelBindings. And when a
      get request is done (with rowIndex and columnIndex), we can first fetch the row vector and then return the
      value for the column index.
      */
    } else if(type.equals(TYPE.TEXT)){
      /*TODO: Get the TermInfo, and fill the columnLabelBindings with <token, autoIncrementArrayIndex> pairs. This
      way, we can return a SparseVector when a row is asked, by fetching ids from columnLabelBindings. And when a get
      request is done (with rowIndex and columnIndex), we can first fetch the row vector and then return the value
      for the column index.
      */
    }
    this.columns = columns;
  }

  public FastIDSet getCandidates(String keyword, int maxLength) throws SolrServerException{
    SolrQuery query = new SolrQuery();
    query.setFacet(false).
      setHighlight(false);

    if(!(type == TYPE.TEXT)){
      if(type == TYPE.BOOLEAN){
        keyword = "true";
      }
      query.setQuery("{!term f="+field+"}"+keyword);
    } else {
      query.setQuery(keyword);
      query.setParam(CommonParams.DF, this.field);
    }
    return getCandidates(query, maxLength);
  }

  public FastIDSet getCandidates(SolrQuery query, int maxLength) throws SolrServerException {
    query.setRows(maxLength).
      setStart(0);
    return getCandidates(query);
  }

  private FastIDSet getCandidates(SolrQuery query) throws SolrServerException {
    FastIDSet idSet = new FastIDSet(query.getRows());
    query.setFields(idField);
    SolrDocumentList docs = server.query(query).getResults();
    for(SolrDocument document:docs) {
      String id = (String)document.getFieldValue(idField);
      idSet.add(Long.parseLong(id));
    }
    return idSet;
  }
  @Override
  public Vector viewRow(int row) {
    //TODO: this should return the document, where the value of the idField is the String representation of the
    //parameter.
    return null;
  }

  @Override
  public Vector viewColumn(int column) {
    String keyword = columnLabelBindings.keySet().toArray(new String[columnLabelBindings.size()])[column];
    try{
      FastIDSet idSet = getCandidates(keyword, rowSize());
      Vector v = new SequentialAccessSparseVector(rowSize());
      for(long id:idSet){
        v.setQuick((int) id, get((int)id, column));
      }
      return v;
    } catch(SolrServerException se){
      return null;
    }
  }

  @Override
  public Matrix assignColumn(int column, Vector other) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Matrix assignRow(int row, Vector other) {
    throw new UnsupportedOperationException();
  }

  @Override
  public double getQuick(int row, int column) {
    return viewRow(row).getQuick(column);
  }

  @Override
  public Matrix like() {
    return new SparseMatrix(rowSize(), columnSize());
  }

  @Override
  public Matrix like(int rows, int columns) {
    return new SparseMatrix(rows, columns);
  }

  @Override
  public void setQuick(int row, int column, double value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Matrix viewPart(int[] offset, int[] size) {
    //TODO: this SHOULD be supported, actually
    throw new UnsupportedOperationException();
  }
}