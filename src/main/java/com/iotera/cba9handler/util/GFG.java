package com.iotera.cba9handler.util;

import java.util.concurrent.ThreadLocalRandom;

public class GFG {
    private static final int[] first_primes_list = {2, 3, 5, 7, 11, 13, 17, 19, 23, 29,
            31, 37, 41, 43, 47, 53, 59, 61, 67,
            71, 73, 79, 83, 89, 97, 101, 103,
            107, 109, 113, 127, 131, 137, 139,
            149, 151, 157, 163, 167, 173, 179,
            181, 191, 193, 197, 199, 211, 223,
            227, 229, 233, 239, 241, 251, 257,
            263, 269, 271, 277, 281, 283, 293,
            307, 311, 313, 317, 331, 337, 347, 349};

    static long nBitRandom(int n)
    {
        int max = (int)Math.pow(2, n) - 1;
        int min = (int)Math.pow(2, n - 1) + 1;
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

//    static int getLowLevelPrime(int n)
//    {
//        while (true) {
//            //  Obtain a random number
//            int prime_candidate = (int) nBitRandom(n);
//
//            for (int divisor : first_primes_list)
//            {
//                if (prime_candidate % divisor == 0
//                        && divisor * divisor <= prime_candidate)
//                    break;
//                else
//                    return prime_candidate;
//            }
//        }
//    }

    static int getLowLevelPrime(int n)
    {
        while (true) {
            int prime_candidate = (int) nBitRandom(n);
            for (int divisor : first_primes_list)
            {
                if (divisor * divisor > prime_candidate)
                    return prime_candidate;
                if (prime_candidate % divisor == 0)
                    break;
            }
        }
    }



    static int expMod(int base, int exp, int mod ){
        if (exp == 0) return 1;
        if (exp % 2 == 0){
            return (int)Math.pow( expMod( base, (exp / 2), mod), 2) % mod;
        }
        else {
            return (base * expMod( base, (exp - 1), mod)) % mod;
        }
    }

    static boolean trialComposite(int round_tester, int evenComponent, int miller_rabin_candidate, int maxDivisionsByTwo)
    {
        if (expMod(round_tester, evenComponent, miller_rabin_candidate) == 1 )
            return false;
        for (int i = 0; i < maxDivisionsByTwo; i++)
        {
            if (expMod(round_tester, (1 << i) * evenComponent, miller_rabin_candidate) == miller_rabin_candidate - 1)
                return false;
        }
        return true;
    }

    static boolean isMillerRabinPassed(int miller_rabin_candidate)
    {
        int maxDivisionsByTwo = 0;
        int evenComponent = miller_rabin_candidate-1;

        while (evenComponent % 2 == 0)
        {
            evenComponent >>= 1;
            maxDivisionsByTwo += 1;
        }

        int numberOfRabinTrials = 20;
        for (int i = 0; i < (numberOfRabinTrials) ; i++)
        {
            int round_tester = ThreadLocalRandom.current().nextInt(2, miller_rabin_candidate + 1);
            if (trialComposite(round_tester, evenComponent, miller_rabin_candidate, maxDivisionsByTwo))
                return false;
        }
        return true;
    }

    public static long GenPrime(int n){
        while (true){
            int prime_candidate = getLowLevelPrime(n);
            if (!isMillerRabinPassed(prime_candidate)){
                continue;
            } else {
                return prime_candidate;
            }
        }


    }
}
