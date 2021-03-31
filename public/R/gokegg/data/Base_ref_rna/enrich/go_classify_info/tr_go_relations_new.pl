#!usr/bin/perl -w

use strict;

die "usage:perl $0 <gene_ontology.1_2.obo> <GO.list>\n" unless @ARGV == 2;

open A,$ARGV[0];
open B,$ARGV[1];

my %h1;my %tar;my %rela;my %h2;my %func;

my %tr=(
    "is" => '1',
    "part_of" => '2',
    "regulates" => '3',
    "negatively_regulates" => '4',
    "positively_regulates" => '5'
);
$/="Term]\nid: ";
<A>;
while(<A>){
    chomp;my @a=split /\n+/;
    my @b=split /: /,$a[1]; #function
    my @c=split /: /,$a[2]; #main function
    #print "$a[0]\t$b[1]\t$c[1]\n";
    $func{$a[0]}=$c[1];
    foreach my $k1(3..$#a){
        my @dd=split /\s+/,$a[$k1];
        if($a[$k1]=~/is_a:/){
            $h1{$a[0]}{$dd[1]}="is";
        }
        if($a[$k1]=~/^relationship:/){
            #$h1{$a[0]}{$dd[2]}=$dd[1];
        }
    }
}
close A;
$/="\n";

while(<B>){
    chomp;my @a=split;
    $tar{$a[0]}=$a[0];
    foreach my $kk(1..20){
        foreach my $k2(sort keys %tar){
            if(exists $h1{$k2}){
                foreach my $k4(sort keys %{$h1{$k2}}){
                    print "$k2\t$k4\n";
                    #$rela{$k2}{$k4}{$h1{$k2}{$k4}}=1;
                    $tar{$k4}=1;
                }
                delete $tar{$k2};
            }
        }
    }
}
close B;
=c
foreach my $k1(sort keys %rela){
    foreach my $k2(sort keys %{$rela{$k1}}){
        foreach my $k3(sort keys %{$rela{$k1}{$k2}}){
            print "$k1\t$k2\t$tr{$k3}\n";
        }
    }
}
