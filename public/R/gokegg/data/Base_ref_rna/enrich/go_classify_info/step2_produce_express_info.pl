#!usr/bin/perl -w

use strict;

die "usage:perl $0 <go2.list> <rela.list> <all go.list> <gene express>\n" unless @ARGV == 4;

open A,$ARGV[0];
open B,$ARGV[1];
open C,$ARGV[2];
open D,$ARGV[3];
my %go;my %un;my %tar;my %exp;

while(<A>){
    chomp;my @a=split;
    $go{$a[0]}=1;$tar{$a[0]}[0]=0;$tar{$a[0]}[1]=0;
}
close A;

while(<B>){
    chomp;my @a=split;
    if(!exists $go{$a[0]}){
        $go{$a[0]}=2;$tar{$a[0]}[0]=0;$tar{$a[0]}[1]=0;
    }
    if(!exists $go{$a[1]}){
        $go{$a[1]}=2;$tar{$a[1]}[0]=0;$tar{$a[1]}[1]=0;
    }
}
close B;

while(<D>){
    chomp;my @a=split;
    $exp{$a[0]}=[@a];
}
close A;
while(<C>){
    chomp;my @a=split;
    my @b=split /;/,$a[1];
    foreach my $k(@b){
        if(exists $go{$k} && exists $exp{$a[0]} && $exp{$a[0]}[-1] < 1){
            $tar{$k}[0]++;
            if($exp{$a[0]}[1] > 0){
                $tar{$k}[1]++;
            }
        }
    }
}
close C;

foreach my $k(keys %go){
    if($tar{$k}[0] == 0){
        print "$k\t$tar{$k}[0]\t0\t0\n";
    }
    else{
        my $rate=$tar{$k}[1]/$tar{$k}[0];
        print "$k\t$tar{$k}[0]\t$go{$k}\t$rate\n";
    }
}
