from domain.ErrorTypes import ErrorTypes
from validity import IncomingEdgeValidityChecker
from utils.code_generation import CodeGenerationUtils
from domain.SharedFunctionTypes import SharedFunctionTypes

import os

def generate_code(args):
    node = args["node"]
    requireds_info = args["requireds_info"]
    edges = args["edges"]

    checklist={"df_count": {1}, "model_count": {0}}
    error, extra=IncomingEdgeValidityChecker.check_validity(node["id"], requireds_info, edges, checklist)
    final_code=[]
    shared_function_set = set()
    additional_local_code = []
    errors = []
    if(error == ErrorTypes.NO_ERROR):
        if ("portion" in extra["dfs"][0]):
            df_name = "df_" + extra["dfs"][0]["source_id"] + "[" + str(extra["dfs"][0]["portion"]) + "]"
        else:
            df_name = "df_" + extra["dfs"][0]["source_id"]

        my_args = {"node_id": node["id"], "input_dfs": [df_name], "shared_function_set": shared_function_set, "additional_local_code": additional_local_code, "errors": errors}
        gen_code=[]

        shared_function_set.add(SharedFunctionTypes.SELECT_EXPR_HELPERS)
        gen_code.append("df_" + node["id"] + "=" + df_name + ".selectExpr(")

        for expr in node["parameters"]["expressions"]["value"]:
            gen_code.extend(['single_select_expr_generator('+ CodeGenerationUtils.handle_parameter(expr["input_cols"], my_args) +', ' + CodeGenerationUtils.handle_parameter(expr["output_cols"], my_args) + ', '+ CodeGenerationUtils.handle_parameter(expr["operation"], my_args) +')', ', '])

        gen_code.pop()
        gen_code.extend([")", os.linesep])

        final_code = CodeGenerationUtils.merge_with_additional_code(gen_code, additional_local_code)

    return final_code, shared_function_set, error