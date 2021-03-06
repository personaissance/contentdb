package com.personaissance.persona.contentdb.matrix;

import com.google.common.collect.Maps;
import org.apache.mahout.math.AbstractMatrix;
import org.apache.mahout.math.CardinalityException;
import org.apache.mahout.math.Matrix;
import org.apache.mahout.math.Vector;

import java.util.Map;

/**
 * Adjoins multiple matrices, rows can be reached as if it is a single matrix
 */
public class SuperMatrix extends AbstractMatrix {

  private Matrix[] matrices;
  private int[] columnSizes;

  public SuperMatrix(Matrix[] matrices) {
    super(matrices[0].rowSize(), columnSize(matrices));
    this.matrices = matrices;

    for (Matrix m : matrices) {
      if (m.rowSize() != rowSize()) {
        throw new CardinalityException(rowSize(), m.rowSize());
      }
    }
    columnSizes = new int[matrices.length];

    for (int i = 0; i < matrices.length; i++) {
      columnSizes[i] = matrices[i].columnSize();
    }

    this.columnLabelBindings = Maps.newHashMap();
    int[] offsets = new int[matrices.length];
    offsets[0] = 0;
    for(int i = 1; i<offsets.length; i++) {
      offsets[i] = offsets[i-1]+matrices[i-1].columnSize();
    }


    for(int i = 0; i<matrices.length; i++) {
      Map<String, Integer> subColumnLabelBindings = matrices[i].getColumnLabelBindings();
      if(subColumnLabelBindings!=null){
        for(Map.Entry<String, Integer> columnAndLabel:subColumnLabelBindings.entrySet()){
          this.columnLabelBindings.put(columnAndLabel.getKey(), columnAndLabel.getValue()+offsets[i]);
        }
      }
    }


  }

  @Override
  public Matrix assignColumn(int column, Vector other) {
    int[] indexes = findMatrixAndColumn(column);
    matrices[indexes[0]].assignColumn(indexes[1], other);
    return this;
  }

  @Override
  public Matrix assignRow(int row, Vector other) {
    int offset = 0;
    for (int i = 0; i < matrices.length; i++) {
      matrices[i].assignRow(row, other.viewPart(offset, columnSizes[i]));
      offset += columnSizes[i];
    }
    return this;
  }

  @Override
  public double getQuick(int row, int column) {
    int[] indexes = findMatrixAndColumn(column);
    return matrices[indexes[0]].getQuick(row, indexes[1]);
  }

  @Override
  public void setQuick(int row, int column, double value) {
    int[] indexes = findMatrixAndColumn(column);
    matrices[indexes[0]].setQuick(row, indexes[1], value);
  }

  @Override
  public Matrix viewPart(int[] offset, int[] size) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Matrix like() {
    Matrix[] matrices = new Matrix[this.matrices.length];
    for (int i = 0; i < matrices.length; i++) {
      matrices[i] = this.matrices[i].like(rowSize(), columnSizes[i]);
    }
    return new SuperMatrix(matrices);
  }

  @Override
  public Matrix like(int rows, int columns) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Vector viewRow(int row) {
//    return new VectorView(super.viewRow(row), 0, columnSize());
    int cardinality = this.columnSize();
    Vector[] vectors = new Vector[matrices.length];
    for (int i = 0; i < matrices.length; i++) {
      vectors[i] = matrices[i].viewRow(row);
    }
    return new VectorSuperView(cardinality, vectors);
  }

  @Override
  public Vector viewColumn(int column) {
    int[] indexes = findMatrixAndColumn(column);
    return matrices[indexes[0]].viewColumn(indexes[1]);
  }

  private int[] findMatrixAndColumn(int column) {
    int i = 0;
    int ithColumnSize = columnSizes[0];
    while (column >= ithColumnSize) {
      column -= ithColumnSize;
      ithColumnSize = columnSizes[++i];
    }
    return new int[]{i, column};
  }


  private static int columnSize(Matrix[] matrices) {
    int columnSize = 0;

    for (Matrix m : matrices) {
      columnSize += m.columnSize();
    }
    return columnSize;
  }
}

