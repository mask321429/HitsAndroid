package com.example.myapplication.polish

class PolishString constructor(expression: String, variables: MutableMap<String, Int>, arrays: MutableMap<String, MutableList<Int>>) {
    var expression: String
    var isExpressionCorrect: Boolean = true

    init {
        this.expression = expression.filter { !it.isWhitespace() }

        this.expression = turnToReversePolish(this.expression, variables, arrays)
    }

    private fun turnToReversePolish(str: String, variables: MutableMap<String, Int>, arrays: MutableMap<String, MutableList<Int>>): String {
        var reversePolish = ""
        val stack = Stack()

        val letterArray = arrayListOf<Char>()
        for (i in 48..57)
            letterArray.add(Char(i))
        for (i in 65..90)
            letterArray.add(Char(i))
        for (i in 97..122)
            letterArray.add(Char(i))
        letterArray.add(Char(95))

        var strWhithArr = str
        val regex = Regex("[a-zA-Z_][a-zA-Z_0-9]*")
        val matches = regex.findAll(str)
        for (key in matches){
            if (arrays.containsKey(key.value)) {
                var sizeArr = arrays[key.value]!!.size
                for (i in variables.keys){
                    if(variables[i]!! < sizeArr)
                        strWhithArr = "${key.value}\\[$i\\]".toRegex().replace(strWhithArr, arrays[key.value]!![variables[i]!!].toString())
                }
                for (i in 0..(sizeArr-1)){
                    var ind = i.toString()


                    strWhithArr = "${key.value}\\[$ind\\]".toRegex().replace(strWhithArr, arrays[key.value]!![i].toString())
                }
            }
        }

        for (char in strWhithArr) {
            if (char in letterArray) {
                reversePolish += char
            } else if(reversePolish.isNotEmpty()){
                when (char) {
                    '+', '-', '*', '/', '%', '(' -> {
                        if (reversePolish.last() in letterArray)
                            reversePolish += ','
                        reversePolish += stack.push(char)
                    }
                    ')' -> {
                        if (reversePolish.last() in letterArray)
                            reversePolish += ','
                        reversePolish += stack.closeBracket()
                    }

                    else -> this.isExpressionCorrect = false
                }
            }
        }
        while (!stack.isEmpty()) {
            reversePolish += ','
            reversePolish += stack.arrayOfChars.removeAt(stack.arrayOfChars.size - 1)
        }

        return "$reversePolish,"
    }
}