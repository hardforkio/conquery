import { combineReducers } from "redux";

import { OPEN_UPLOAD_MODAL, CLOSE_UPLOAD_MODAL } from "./actionTypes";
import createQueryRunnerReducer from "../../query-runner/reducer";

function isModalOpen(state: boolean = false, action: Object): boolean {
  switch (action.type) {
    case OPEN_UPLOAD_MODAL:
      return true;
    case CLOSE_UPLOAD_MODAL:
      return false;
    default:
      return state;
  }
}

const queryRunner = createQueryRunnerReducer("external");

export default combineReducers({
  isModalOpen,
  queryRunner
});
