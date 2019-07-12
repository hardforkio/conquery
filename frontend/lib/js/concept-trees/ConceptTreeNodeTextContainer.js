// @flow

import React from "react";
import styled from "@emotion/styled";
import { css } from "@emotion/core";
import { findDOMNode } from "react-dom";
import { DragSource } from "react-dnd";

import { AdditionalInfoHoverable } from "../tooltip";
import { isEmpty } from "../common/helpers";
import { dndTypes } from "../common/constants";

import { type AdditionalInfoHoverableNodeType } from "../tooltip/AdditionalInfoHoverable";
import { type DraggedNodeType } from "../standard-query-editor/types";
import { type SearchType } from "./reducer";

import ConceptTreeNodeText from "./ConceptTreeNodeText";

type PropsType = {
  node: AdditionalInfoHoverableNodeType & {
    label: string,
    description?: string,
    matchingEntries?: number
  },
  open: boolean,
  depth: number,
  active?: boolean,
  onTextClick?: Function,
  createQueryElement: () => DraggedNodeType,
  connectDragSource: Function,
  search?: SearchType,
  isStructFolder?: boolean
};

function getResultCount(search, node) {
  return search.result &&
    search.result[node.id] > 0 &&
    (node.children && node.children.some(child => search.result[child] > 0))
    ? search.result[node.id]
    : null;
}

// Has to be a class because of https://github.com/react-dnd/react-dnd/issues/530
class ConceptTreeNodeTextContainer extends React.Component {
  render() {
    const {
      node,
      depth,
      search,
      active,
      open,
      connectDragSource,
      onTextClick,
      isStructFolder
    } = this.props;

    const red = !isEmpty(node.matchingEntries) && node.matchingEntries === 0;
    const resultCount = getResultCount(search, node);
    const hasChildren = !!node.children && node.children.length > 0;

    return (
      <ConceptTreeNodeText
        ref={instance => {
          // Don't allow dragging with inactive elements
          if (active !== false) {
            connectDragSource(instance);
          }
        }}
        label={node.label}
        depth={depth}
        description={node.description}
        resultCount={resultCount}
        searchWords={search.words}
        hasChildren={hasChildren}
        isOpen={open}
        isStructFolder={isStructFolder}
        red={red}
        depth={depth}
        onClick={onTextClick}
      />
    );
  }
}

/**
 * Implements the drag source contract.
 */
const nodeSource = {
  beginDrag(props: PropsType, monitor, component): DraggedNodeType {
    const { width, height } = findDOMNode(component).getBoundingClientRect();

    return {
      width,
      height,
      ...props.createQueryElement()
    };
  }
};

/**
 * Specifies the props to inject into your component.
 */
const collect = (connect, monitor) => ({
  connectDragSource: connect.dragSource(),
  isDragging: monitor.isDragging()
});

const DraggableConceptTreeNodeTextContainer = DragSource(
  dndTypes.CONCEPT_TREE_NODE,
  nodeSource,
  collect
)(ConceptTreeNodeTextContainer);

export default AdditionalInfoHoverable(DraggableConceptTreeNodeTextContainer);