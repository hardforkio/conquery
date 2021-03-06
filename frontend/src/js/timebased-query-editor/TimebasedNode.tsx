import React from "react";
import { findDOMNode } from "react-dom";
import T from "i18n-react";
import styled from "@emotion/styled";
import { DragSource } from "react-dnd";

import VerticalToggleButton, {
  Option
} from "../form-components/VerticalToggleButton";
import {
  EARLIEST,
  LATEST,
  RANDOM
} from "../common/constants/timebasedQueryTimestampTypes";
import { TIMEBASED_NODE } from "../common/constants/dndTypes";

import IconButton from "../button/IconButton";

const StyledIconButton = styled(IconButton)`
  position: absolute;
  top: 0;
  right: 0;
  z-index: 1;
`;

const Root = styled("div")`
  margin: 0 5px;
  width: 200px;
  font-size: ${({ theme }) => theme.font.sm};
`;

const StyledVerticalToggleButton = styled(VerticalToggleButton)`
  ${Option} {
    border: 0;

    &:first-of-type,
    &:last-of-type {
      border-radius: 0;
    }
  }
`;

type PropsType = {
  node: Object;
  position: "left" | "right";
  isIndexResult: boolean;
  onRemove: Function;
  onSetTimebasedNodeTimestamp: Function;
  onSetTimebasedIndexResult: Function;
  conditionIdx: number;
  resultIdx: number;
  connectDragSource: Function;
  isIndexResultDisabled: boolean;
};

// Has to be a class because of https://github.com/react-dnd/react-dnd/issues/530
class TimebasedNode extends React.Component {
  props: PropsType;

  render() {
    const {
      node,
      connectDragSource,
      // isIndexResult,
      // isIndexResultDisabled,
      onRemove,
      // onSetTimebasedIndexResult,
      onSetTimebasedNodeTimestamp
    } = this.props;

    const toggleButton = (
      <StyledVerticalToggleButton
        onToggle={onSetTimebasedNodeTimestamp}
        activeValue={node.timestamp}
        options={[
          {
            label: T.translate("timebasedQueryEditor.timestampFirst"),
            value: EARLIEST
          },
          {
            label: T.translate("timebasedQueryEditor.timestampRandom"),
            value: RANDOM
          },
          {
            label: T.translate("timebasedQueryEditor.timestampLast"),
            value: LATEST
          }
        ]}
      />
    );

    return (
      <Root
        ref={instance => {
          connectDragSource(instance);
        }}
      >
        <div className="timebased-node__container">
          <div className="timebased-node__content">
            <div className="timebased-node__timestamp">
              <p className="timebased-node__timestamp__title">
                {T.translate("timebasedQueryEditor.timestamp")}
              </p>
              {toggleButton}
            </div>
            <div className="timebased-node__description">
              <StyledIconButton icon="times" onClick={onRemove} />
              <p className="timebased-node__description__text">
                {node.label || node.id}
              </p>
            </div>
          </div>
        </div>
      </Root>
    );
  }
}

// Button indexResult (to re-enable this soon)
// <button
//   className={classnames("timebased-node__index-result-btn", {
//     "timebased-node__index-result-btn--active": isIndexResult,
//     "timebased-node__index-result-btn--disabled": isIndexResultDisabled
//   })}
//   disabled={isIndexResultDisabled}
//   onClick={onSetTimebasedIndexResult}
// >
//   {T.translate("timebasedQueryEditor.timestampResultsFrom")}
// </button>

/**
 * Implements the drag source contract.
 */
const nodeSource = {
  beginDrag(props, monitor, component) {
    // Return the data describing the dragged item
    const { node, conditionIdx, resultIdx } = props;
    const { width, height } = findDOMNode(component).getBoundingClientRect();

    return {
      width,
      height,
      conditionIdx,
      resultIdx,
      node,
      moved: true
    };
  }
};

/**
 * Specifies the dnd-related props to inject into the component.
 */
function collect(connect, monitor) {
  return {
    connectDragSource: connect.dragSource(),
    isDragging: monitor.isDragging()
  };
}

const DraggableTimebasedNode = DragSource(
  TIMEBASED_NODE,
  nodeSource,
  collect
)(TimebasedNode);

export default DraggableTimebasedNode;
