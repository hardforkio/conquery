// @flow

import React from "react";
import T from "i18n-react";
import { type FieldPropsType } from "redux-form";

import ReactSelect from "./ReactSelect";
import Labeled from "./Labeled";

import { isEmpty } from "../common/helpers";
import { type SelectOptionsType } from "../common/types/backend";
import InfoTooltip from "../tooltip/InfoTooltip";

type PropsType = FieldPropsType & {
  label: string,
  options: SelectOptionsType,
  disabled?: boolean,
  selectProps?: Object,
  tooltip?: string
};

const InputSelect = (props: PropsType) => {
  const { input, options } = props;
  const selected = options && options.filter(v => v.value === input.value);
  const defaultValue =
    options && options.filter(v => v.value === input.defaultValue);

  return (
    <Labeled
      disabled={props.disabled}
      valueChanged={!isEmpty(input.value) && input.value !== input.defaultValue}
      label={
        <>
          {props.label}
          {props.tooltip && <InfoTooltip text={props.tooltip} />}
        </>
      }
    >
      <ReactSelect
        highlightChanged
        name="form-field"
        value={selected}
        defaultValue={defaultValue}
        options={options}
        onChange={field =>
          field ? input.onChange(field.value) : input.onChange(null)
        }
        isSearchable={false}
        isClearable={input.clearable}
        isDisabled={!!props.disabled}
        placeholder={T.translate("reactSelect.placeholder")}
        noOptionsMessage={() => T.translate("reactSelect.noResults")}
        {...props.selectProps}
      />
    </Labeled>
  );
};

export default InputSelect;
