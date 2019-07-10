// @flow

import { combineReducers } from "redux";
import { reducer as reduxFormReducer } from "redux-form";

import { SET_EXTERNAL_FORM } from "./actionTypes";

import { createQueryRunnerReducer } from "conquery/lib/js/query-runner";

const buildExternalFormsReducer = (availableForms: Object) => {
  const forms = Object.values(availableForms);

  // collect reducers from form extension
  const formReducers = Object.assign(
    {},
    ...forms.map(form => ({ [form.type]: form.reducer }))
  );

  const defaultForm = forms.length
    ? forms.sort((a, b) => a.order - b.order)[0]
    : null;

  const activeFormReducer = (
    state: string = defaultForm.type,
    action: Object
  ): string => {
    switch (action.type) {
      case SET_EXTERNAL_FORM:
        return action.payload.form;
      default:
        return state;
    }
  };

  return combineReducers({
    activeForm: activeFormReducer,

    // Redux-Form reducer to keep the state of all forms:
    reduxForm: reduxFormReducer,

    // Query Runner reducer that works with external forms
    queryRunner: createQueryRunnerReducer("externalForms"),

    // TODO: Because availableForms is an EVA-internal constant, we could remove it from the state
    availableForms: (state = availableForms) => state,

    ...formReducers
  });
};

export default buildExternalFormsReducer;