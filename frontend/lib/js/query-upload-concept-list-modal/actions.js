// @flow

import {
  initUploadConceptListModal,
  resetUploadConceptListModal
} from "../upload-concept-list-modal/actions";
import { MODAL_OPEN, MODAL_CLOSE, MODAL_ACCEPT } from "./actionTypes";

const openModal = (andIdx = null) => ({
  type: MODAL_OPEN,
  payload: { andIdx }
});

export const openQueryUploadConceptListModal = (andIdx, file) => dispatch => {
  return dispatch([initUploadConceptListModal(file), openModal(andIdx)]);
};

const closeModal = () => ({
  type: MODAL_CLOSE
});

export const closeQueryUploadConceptListModal = () => dispatch => {
  return dispatch([closeModal(), resetUploadConceptListModal()]);
};

export const acceptQueryUploadConceptListModal = (
  andIdx,
  label,
  rootConcepts,
  resolvedConcepts
) => {
  return {
    type: MODAL_ACCEPT,
    payload: { andIdx, label, rootConcepts, resolvedConcepts }
  };
};